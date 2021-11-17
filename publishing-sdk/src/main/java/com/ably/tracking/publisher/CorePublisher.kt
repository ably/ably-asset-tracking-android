package com.ably.tracking.publisher

import android.Manifest
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionException
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.common.logging.w
import com.ably.tracking.logging.LogHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface CorePublisher {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request<*>)
    val locations: SharedFlow<LocationUpdate>
    val trackables: SharedFlow<Set<Trackable>>
    val locationHistory: SharedFlow<LocationHistoryData>
    val active: Trackable?
    val routingProfile: RoutingProfile
    val trackableStateFlows: Map<String, StateFlow<TrackableState>>
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
internal fun createCorePublisher(
    ably: Ably,
    mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile,
    logHandler: LogHandler?,
    areRawLocationsEnabled: Boolean?,
    sendResolutionEnabled: Boolean,
): CorePublisher {
    return DefaultCorePublisher(
        ably,
        mapbox,
        resolutionPolicyFactory,
        routingProfile,
        logHandler,
        areRawLocationsEnabled,
        sendResolutionEnabled,
    )
}

/**
 * This is a private static single thread dispatcher that will be used for all the [Publisher] instances.
 */
private val singleThreadDispatcher = createSingleThreadDispatcher()

private class DefaultCorePublisher
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
constructor(
    private val ably: Ably,
    private val mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile,
    private val logHandler: LogHandler?,
    private val areRawLocationsEnabled: Boolean?,
    private val sendResolutionEnabled: Boolean,
) : CorePublisher {
    private val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _locations = MutableSharedFlow<LocationUpdate>(replay = 1)
    private val _trackables = MutableSharedFlow<Set<Trackable>>(replay = 1)
    private val _locationHistory = MutableSharedFlow<LocationHistoryData>()
    private val thresholdChecker = ThresholdChecker()
    private val policy: ResolutionPolicy
    private val hooks = Hooks()
    private val methods = Methods()
    override val locations: SharedFlow<LocationUpdate>
        get() = _locations.asSharedFlow()
    override val trackables: SharedFlow<Set<Trackable>>
        get() = _trackables.asSharedFlow()
    override val locationHistory: SharedFlow<LocationHistoryData>
        get() = _locationHistory.asSharedFlow()

    override var active: Trackable? = null
    override var routingProfile: RoutingProfile = routingProfile
    override var trackableStateFlows: Map<String, StateFlow<TrackableState>> = emptyMap()

    init {
        policy = resolutionPolicyFactory.createResolutionPolicy(
            hooks,
            methods
        )
        val channel = Channel<Event>()
        sendEventChannel = channel
        scope.launch {
            coroutineScope {
                sequenceEventsQueue(channel, routingProfile)
            }
        }
        ably.subscribeForAblyStateChange { enqueue(AblyConnectionStateChangeEvent(it)) }
        mapbox.registerLocationObserver(object : LocationUpdatesObserver {
            override fun onRawLocationChanged(rawLocation: Location) {
                enqueue(RawLocationChangedEvent(rawLocation))
            }

            override fun onEnhancedLocationChanged(enhancedLocation: Location, intermediateLocations: List<Location>) {
                val locationUpdateType =
                    if (intermediateLocations.isEmpty()) LocationUpdateType.ACTUAL else LocationUpdateType.PREDICTED
                enqueue(EnhancedLocationChangedEvent(enhancedLocation, intermediateLocations, locationUpdateType))
            }
        })
        mapbox.setLocationHistoryListener { historyData -> scope.launch { _locationHistory.emit(historyData) } }
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request<*>) {
        scope.launch { sendEventChannel.send(request) }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun CoroutineScope.sequenceEventsQueue(
        receiveEventChannel: ReceiveChannel<Event>,
        routingProfile: RoutingProfile
    ) {
        launch {
            // state
            val state = State(routingProfile, policy.resolve(emptySet()), areRawLocationsEnabled)

            // processing
            for (event in receiveEventChannel) {
                // handle events after the publisher is stopped
                if (state.isStopped) {
                    if (event is Request<*>) {
                        // when the event is a request then call its handler
                        when (event) {
                            is StopEvent -> event.handler(Result.success(Unit))
                            else -> event.handler(Result.failure(PublisherStoppedException()))
                        }
                        continue
                    } else if (event is AdhocEvent) {
                        // when the event is an adhoc event then just ignore it
                        continue
                    }
                }
                when (event) {
                    is SetDestinationSuccessEvent -> {
                        state.estimatedArrivalTimeInMilliseconds =
                            System.currentTimeMillis() + event.routeDurationInMilliseconds
                    }
                    is RawLocationChangedEvent -> {
                        state.lastPublisherLocation = event.location
                        if (areRawLocationsEnabled == true) {
                            state.trackables.forEach { processRawLocationUpdate(event, state, it.id) }
                        }
                        state.rawLocationChangedCommands.apply {
                            if (isNotEmpty()) {
                                forEach { command -> command(state) }
                                clear()
                            }
                        }
                    }
                    is EnhancedLocationChangedEvent -> {
                        state.trackables.forEach { processEnhancedLocationUpdate(event, state, it.id) }
                        scope.launch {
                            _locations.emit(
                                EnhancedLocationUpdate(
                                    event.location,
                                    emptyList(),
                                    event.intermediateLocations,
                                    event.type
                                )
                            )
                        }
                        checkThreshold(
                            event.location,
                            state.active,
                            state.estimatedArrivalTimeInMilliseconds
                        )
                    }
                    is TrackTrackableEvent -> {
                        request(
                            AddTrackableEvent(event.trackable) { result ->
                                if (result.isSuccess) {
                                    request(SetActiveTrackableEvent(event.trackable) { event.handler(result) })
                                } else {
                                    event.handler(result)
                                }
                            }
                        )
                    }
                    is SetActiveTrackableEvent -> {
                        if (state.active != event.trackable) {
                            state.active = event.trackable
                            hooks.trackables?.onActiveTrackableChanged(event.trackable)
                            event.trackable.destination?.let {
                                setDestination(it, state)
                            }
                        }
                        event.handler(Result.success(Unit))
                    }
                    is AddTrackableEvent -> {
                        when {
                            state.duplicateTrackableGuard.isCurrentlyAddingTrackable(event.trackable) -> {
                                state.duplicateTrackableGuard.saveDuplicateAddHandler(event.trackable, event.handler)
                            }
                            state.trackables.contains(event.trackable) -> {
                                event.handler(Result.success(state.trackableStateFlows[event.trackable.id]!!))
                            }
                            else -> {
                                state.duplicateTrackableGuard.startAddingTrackable(event.trackable)
                                ably.connect(event.trackable.id, state.presenceData, willPublish = true) { result ->
                                    try {
                                        result.getOrThrow()
                                        request(ConnectionForTrackableCreatedEvent(event.trackable, event.handler))
                                    } catch (exception: ConnectionException) {
                                        request(AddTrackableFailedEvent(event.trackable, event.handler, exception))
                                    }
                                }
                            }
                        }
                    }
                    is AddTrackableFailedEvent -> {
                        val failureResult = Result.failure<AddTrackableResult>(event.exception)
                        event.handler(failureResult)
                        state.duplicateTrackableGuard.finishAddingTrackable(event.trackable, failureResult)
                    }
                    is PresenceMessageEvent -> {
                        when (event.presenceMessage.action) {
                            PresenceAction.PRESENT_OR_ENTER -> {
                                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                                    addSubscriber(
                                        event.presenceMessage.clientId,
                                        event.trackable,
                                        event.presenceMessage.data,
                                        state
                                    )
                                }
                            }
                            PresenceAction.LEAVE_OR_ABSENT -> {
                                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                                    removeSubscriber(event.presenceMessage.clientId, event.trackable, state)
                                }
                            }
                            PresenceAction.UPDATE -> {
                                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                                    updateSubscriber(
                                        event.presenceMessage.clientId,
                                        event.trackable,
                                        event.presenceMessage.data,
                                        state
                                    )
                                }
                            }
                        }
                    }
                    is ConnectionForTrackableCreatedEvent -> {
                        ably.subscribeForPresenceMessages(
                            trackableId = event.trackable.id,
                            listener = { enqueue(PresenceMessageEvent(event.trackable, it)) },
                            callback = { result ->
                                try {
                                    result.getOrThrow()
                                    request(ConnectionForTrackableReadyEvent(event.trackable, event.handler))
                                } catch (exception: ConnectionException) {
                                    ably.disconnect(event.trackable.id, state.presenceData) {
                                        request(AddTrackableFailedEvent(event.trackable, event.handler, exception))
                                    }
                                }
                            }
                        )
                    }
                    is ConnectionForTrackableReadyEvent -> {
                        ably.subscribeForChannelStateChange(event.trackable.id) {
                            enqueue(ChannelConnectionStateChangeEvent(it, event.trackable.id))
                        }
                        if (!state.isTracking) {
                            state.isTracking = true
                            mapbox.startTrip()
                        }
                        state.trackables.add(event.trackable)
                        scope.launch { _trackables.emit(state.trackables) }
                        resolveResolution(event.trackable, state)
                        hooks.trackables?.onTrackableAdded(event.trackable)
                        val trackableState = state.trackableStates[event.trackable.id] ?: TrackableState.Offline()
                        val trackableStateFlow =
                            state.trackableStateFlows[event.trackable.id] ?: MutableStateFlow(trackableState)
                        state.trackableStateFlows[event.trackable.id] = trackableStateFlow
                        trackableStateFlows = state.trackableStateFlows
                        state.trackableStates[event.trackable.id] = trackableState
                        val successResult = Result.success(trackableStateFlow.asStateFlow())
                        event.handler(successResult)
                        state.duplicateTrackableGuard.finishAddingTrackable(event.trackable, successResult)
                    }
                    is ChangeLocationEngineResolutionEvent -> {
                        state.locationEngineResolution = policy.resolve(state.resolutions.values.toSet())
                        mapbox.changeResolution(state.locationEngineResolution)
                    }
                    is RemoveTrackableEvent -> {
                        if (state.trackables.contains(event.trackable)) {
                            // Leave Ably channel.
                            ably.disconnect(event.trackable.id, state.presenceData) { result ->
                                if (result.isSuccess) {
                                    request(
                                        DisconnectSuccessEvent(event.trackable) {
                                            if (it.isSuccess) {
                                                event.handler(Result.success(true))
                                            } else {
                                                event.handler(Result.failure(it.exceptionOrNull()!!))
                                            }
                                        }
                                    )
                                } else {
                                    event.handler(Result.failure(result.exceptionOrNull()!!))
                                }
                            }
                        } else {
                            // notify with false to indicate that it was not removed
                            event.handler(Result.success(false))
                        }
                    }
                    is DisconnectSuccessEvent -> {
                        state.trackables.remove(event.trackable)
                        scope.launch { _trackables.emit(state.trackables) }
                        state.trackableStateFlows.remove(event.trackable.id) // there is no way to stop the StateFlow so we just remove it
                        trackableStateFlows = state.trackableStateFlows
                        state.trackableStates.remove(event.trackable.id)
                        hooks.trackables?.onTrackableRemoved(event.trackable)
                        removeAllSubscribers(event.trackable, state)
                        state.resolutions.remove(event.trackable.id)
                            ?.let { enqueue(ChangeLocationEngineResolutionEvent()) }
                        state.requests.remove(event.trackable.id)
                        state.lastSentEnhancedLocations.remove(event.trackable.id)
                        state.lastSentRawLocations.remove(event.trackable.id)
                        state.skippedEnhancedLocations.clear(event.trackable.id)
                        state.skippedRawLocations.clear(event.trackable.id)
                        state.enhancedLocationsPublishingState.clear(event.trackable.id)
                        state.rawLocationsPublishingState.clear(event.trackable.id)
                        state.duplicateTrackableGuard.clear(event.trackable)
                        // If this was the active Trackable then clear that state and remove destination.
                        if (state.active == event.trackable) {
                            removeCurrentDestination(state)
                            state.active = null
                            hooks.trackables?.onActiveTrackableChanged(null)
                        }
                        // When we remove the last trackable then we should stop location updates
                        if (state.trackables.isEmpty() && state.isTracking) {
                            stopLocationUpdates(state)
                        }
                        state.lastChannelConnectionStateChanges.remove(event.trackable.id)
                        event.handler(Result.success(Unit))
                    }
                    is RefreshResolutionPolicyEvent -> {
                        state.trackables.forEach { resolveResolution(it, state) }
                    }
                    is ChangeRoutingProfileEvent -> {
                        state.routingProfile = event.routingProfile
                        state.currentDestination?.let { setDestination(it, state) }
                    }
                    is StopEvent -> {
                        if (state.isTracking) {
                            stopLocationUpdates(state)
                        }
                        try {
                            ably.close(state.presenceData)
                            state.dispose()
                            state.isStopped = true
                            event.handler(Result.success(Unit))
                        } catch (exception: ConnectionException) {
                            event.handler(Result.failure(exception))
                        }
                    }
                    is AblyConnectionStateChangeEvent -> {
                        state.lastConnectionStateChange = event.connectionStateChange
                        state.trackables.forEach {
                            updateTrackableState(state, it.id)
                        }
                    }
                    is ChannelConnectionStateChangeEvent -> {
                        state.lastChannelConnectionStateChanges[event.trackableId] = event.connectionStateChange
                        updateTrackableState(state, event.trackableId)
                    }
                    is SendEnhancedLocationSuccessEvent -> {
                        state.enhancedLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                        state.lastSentEnhancedLocations[event.trackableId] = event.location
                        state.skippedEnhancedLocations.clear(event.trackableId)
                        updateTrackableState(state, event.trackableId)
                        processNextWaitingEnhancedLocationUpdate(state, event.trackableId)
                    }
                    is SendEnhancedLocationFailureEvent -> {
                        if (state.enhancedLocationsPublishingState.shouldRetryPublishing(event.trackableId)) {
                            retrySendingEnhancedLocation(state, event.trackableId, event.locationUpdate)
                        } else {
                            state.enhancedLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                            saveEnhancedLocationForFurtherSending(
                                state,
                                event.trackableId,
                                event.locationUpdate.location
                            )
                            logHandler?.w(
                                "Sending location update failed. Location saved for further sending",
                                event.exception
                            )
                            processNextWaitingEnhancedLocationUpdate(state, event.trackableId)
                        }
                    }
                    is SendRawLocationSuccessEvent -> {
                        state.rawLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                        state.lastSentRawLocations[event.trackableId] = event.location
                        state.skippedRawLocations.clear(event.trackableId)
                        processNextWaitingRawLocationUpdate(state, event.trackableId)
                    }
                    is SendRawLocationFailureEvent -> {
                        if (state.rawLocationsPublishingState.shouldRetryPublishing(event.trackableId)) {
                            retrySendingRawLocation(state, event.trackableId, event.locationUpdate)
                        } else {
                            state.rawLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                            saveRawLocationForFurtherSending(state, event.trackableId, event.locationUpdate.location)
                            logHandler?.w(
                                "Sending raw location update failed. Location saved for further sending",
                                event.exception
                            )
                            processNextWaitingRawLocationUpdate(state, event.trackableId)
                        }
                    }
                }
            }
        }
    }

    private fun retrySendingEnhancedLocation(
        state: State,
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate
    ) {
        state.enhancedLocationsPublishingState.incrementRetryCount(trackableId)
        sendEnhancedLocationUpdate(
            EnhancedLocationChangedEvent(
                locationUpdate.location,
                locationUpdate.intermediateLocations,
                locationUpdate.type
            ),
            state,
            trackableId
        )
    }

    private fun processEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        state: State,
        trackableId: String
    ) {
        when {
            state.enhancedLocationsPublishingState.hasPendingMessage(trackableId) -> {
                state.enhancedLocationsPublishingState.addToWaiting(trackableId, event)
            }
            shouldSendLocation(
                event.location,
                state.lastSentEnhancedLocations[trackableId],
                state.resolutions[trackableId]
            ) -> {
                sendEnhancedLocationUpdate(event, state, trackableId)
            }
            else -> {
                saveEnhancedLocationForFurtherSending(state, trackableId, event.location)
                processNextWaitingEnhancedLocationUpdate(state, trackableId)
            }
        }
    }

    private fun processNextWaitingEnhancedLocationUpdate(state: State, trackableId: String) {
        state.enhancedLocationsPublishingState.getNextWaiting(trackableId)?.let {
            processEnhancedLocationUpdate(it, state, trackableId)
        }
    }

    private fun sendEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        state: State,
        trackableId: String
    ) {
        val locationUpdate = EnhancedLocationUpdate(
            event.location,
            state.skippedEnhancedLocations.toList(trackableId),
            event.intermediateLocations,
            event.type
        )
        state.enhancedLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendEnhancedLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(SendEnhancedLocationSuccessEvent(locationUpdate.location, trackableId))
            } else {
                enqueue(SendEnhancedLocationFailureEvent(locationUpdate, trackableId, it.exceptionOrNull()))
            }
        }
    }

    private fun saveEnhancedLocationForFurtherSending(state: State, trackableId: String, location: Location) {
        state.skippedEnhancedLocations.add(trackableId, location)
    }

    private fun retrySendingRawLocation(state: State, trackableId: String, locationUpdate: LocationUpdate) {
        state.rawLocationsPublishingState.incrementRetryCount(trackableId)
        sendRawLocationUpdate(RawLocationChangedEvent(locationUpdate.location), state, trackableId)
    }

    private fun processRawLocationUpdate(
        event: RawLocationChangedEvent,
        state: State,
        trackableId: String
    ) {
        when {
            state.rawLocationsPublishingState.hasPendingMessage(trackableId) -> {
                state.rawLocationsPublishingState.addToWaiting(trackableId, event)
            }
            shouldSendLocation(
                event.location,
                state.lastSentRawLocations[trackableId],
                state.resolutions[trackableId]
            ) -> {
                sendRawLocationUpdate(event, state, trackableId)
            }
            else -> {
                saveRawLocationForFurtherSending(state, trackableId, event.location)
                processNextWaitingRawLocationUpdate(state, trackableId)
            }
        }
    }

    private fun processNextWaitingRawLocationUpdate(state: State, trackableId: String) {
        state.rawLocationsPublishingState.getNextWaiting(trackableId)?.let {
            processRawLocationUpdate(it, state, trackableId)
        }
    }

    private fun sendRawLocationUpdate(
        event: RawLocationChangedEvent,
        state: State,
        trackableId: String
    ) {
        val locationUpdate = LocationUpdate(
            event.location,
            state.skippedRawLocations.toList(trackableId),
        )
        state.rawLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendRawLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(SendRawLocationSuccessEvent(locationUpdate.location, trackableId))
            } else {
                enqueue(SendRawLocationFailureEvent(locationUpdate, trackableId, it.exceptionOrNull()))
            }
        }
    }

    private fun saveRawLocationForFurtherSending(state: State, trackableId: String, location: Location) {
        state.skippedRawLocations.add(trackableId, location)
    }

    private fun stopLocationUpdates(state: State) {
        state.isTracking = false
        mapbox.unregisterLocationObserver()
        mapbox.stopAndClose()
    }

    private fun updateTrackableState(state: State, trackableId: String) {
        val hasSentAtLeastOneLocation: Boolean = state.lastSentEnhancedLocations[trackableId] != null
        val lastChannelConnectionStateChange = getLastChannelConnectionStateChange(state, trackableId)
        val newTrackableState = when (state.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE -> if (hasSentAtLeastOneLocation) TrackableState.Online else TrackableState.Offline()
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(state.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newTrackableState != state.trackableStates[trackableId]) {
            state.trackableStates[trackableId] = newTrackableState
            scope.launch { state.trackableStateFlows[trackableId]?.emit(newTrackableState) }
        }
    }

    private fun getLastChannelConnectionStateChange(state: State, trackableId: String): ConnectionStateChange =
        state.lastChannelConnectionStateChanges[trackableId]
            ?: ConnectionStateChange(ConnectionState.OFFLINE, null)

    private fun removeAllSubscribers(trackable: Trackable, state: State) {
        state.subscribers[trackable.id]?.let { subscribers ->
            subscribers.forEach { hooks.subscribers?.onSubscriberRemoved(it) }
            subscribers.clear()
        }
    }

    private fun addSubscriber(id: String, trackable: Trackable, data: PresenceData, state: State) {
        val subscriber = Subscriber(id, trackable)
        if (state.subscribers[trackable.id] == null) {
            state.subscribers[trackable.id] = mutableSetOf()
        }
        state.subscribers[trackable.id]?.add(subscriber)
        saveOrRemoveResolutionRequest(data.resolution, trackable, subscriber, state)
        hooks.subscribers?.onSubscriberAdded(subscriber)
        resolveResolution(trackable, state)
    }

    private fun updateSubscriber(id: String, trackable: Trackable, data: PresenceData, state: State) {
        state.subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                data.resolution.let { resolution ->
                    saveOrRemoveResolutionRequest(resolution, trackable, subscriber, state)
                    resolveResolution(trackable, state)
                }
            }
        }
    }

    private fun removeSubscriber(id: String, trackable: Trackable, state: State) {
        state.subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                subscribers.remove(subscriber)
                state.requests[trackable.id]?.remove(subscriber)
                hooks.subscribers?.onSubscriberRemoved(subscriber)
                resolveResolution(trackable, state)
            }
        }
    }

    private fun saveOrRemoveResolutionRequest(
        resolution: Resolution?,
        trackable: Trackable,
        subscriber: Subscriber,
        state: State
    ) {
        if (resolution != null) {
            if (state.requests[trackable.id] == null) {
                state.requests[trackable.id] = mutableMapOf()
            }
            state.requests[trackable.id]?.put(subscriber, resolution)
        } else {
            state.requests[trackable.id]?.remove(subscriber)
        }
    }

    private fun resolveResolution(trackable: Trackable, state: State) {
        val resolutionRequests: Set<Resolution> = state.requests[trackable.id]?.values?.toSet() ?: emptySet()
        policy.resolve(TrackableResolutionRequest(trackable, resolutionRequests)).let { resolution ->
            state.resolutions[trackable.id] = resolution
            enqueue(ChangeLocationEngineResolutionEvent())
            if (sendResolutionEnabled) {
                ably.sendResolution(trackable.id, resolution) {} // we ignore the result of this operation
            }
        }
    }

    private fun setDestination(destination: Destination, state: State) {
        state.lastPublisherLocation.let { currentLocation ->
            if (currentLocation != null) {
                removeCurrentDestination(state)
                state.currentDestination = destination
                mapbox.setRoute(currentLocation, destination, state.routingProfile) {
                    try {
                        enqueue(SetDestinationSuccessEvent(it.getOrThrow()))
                    } catch (exception: MapException) {
                        logHandler?.w("Setting trackable destination failed", exception)
                    }
                }
            } else {
                state.rawLocationChangedCommands.add { updatedState -> setDestination(destination, updatedState) }
            }
        }
    }

    private fun removeCurrentDestination(state: State) {
        mapbox.clearRoute()
        state.currentDestination = null
        state.estimatedArrivalTimeInMilliseconds = null
    }

    private fun checkThreshold(
        currentLocation: Location,
        activeTrackable: Trackable?,
        estimatedArrivalTimeInMilliseconds: Long?
    ) {
        methods.threshold?.let { threshold ->
            if (thresholdChecker.isThresholdReached(
                    threshold,
                    currentLocation,
                    System.currentTimeMillis(),
                    activeTrackable?.destination,
                    estimatedArrivalTimeInMilliseconds
                )
            ) {
                methods.onProximityReached()
            }
        }
    }

    private fun shouldSendLocation(
        currentLocation: Location,
        lastSentLocation: Location?,
        resolution: Resolution?
    ): Boolean {
        return if (resolution != null && lastSentLocation != null) {
            val timeSinceLastSentLocation = currentLocation.timeFrom(lastSentLocation)
            val distanceFromLastSentLocation = currentLocation.distanceInMetersFrom(lastSentLocation)
            return distanceFromLastSentLocation >= resolution.minimumDisplacement ||
                timeSinceLastSentLocation >= resolution.desiredInterval
        } else {
            true
        }
    }

    private inner class Hooks : ResolutionPolicy.Hooks {
        var trackables: ResolutionPolicy.Hooks.TrackableSetListener? = null
        var subscribers: ResolutionPolicy.Hooks.SubscriberSetListener? = null

        override fun trackables(listener: ResolutionPolicy.Hooks.TrackableSetListener) {
            trackables = listener
        }

        override fun subscribers(listener: ResolutionPolicy.Hooks.SubscriberSetListener) {
            subscribers = listener
        }
    }

    private inner class Methods : ResolutionPolicy.Methods {
        var proximityHandler: ResolutionPolicy.Methods.ProximityHandler? = null
        var threshold: Proximity? = null
        override fun refresh() {
            enqueue(RefreshResolutionPolicyEvent())
        }

        override fun setProximityThreshold(
            threshold: Proximity,
            handler: ResolutionPolicy.Methods.ProximityHandler
        ) {
            this.proximityHandler = handler
            this.threshold = threshold
        }

        override fun cancelProximityThreshold() {
            proximityHandler?.onProximityCancelled()
        }

        fun onProximityReached() {
            threshold?.let { proximityHandler?.onProximityReached(it) }
        }
    }

    private inner class State(
        routingProfile: RoutingProfile,
        locationEngineResolution: Resolution,
        areRawLocationsEnabled: Boolean?,
    ) {
        private var isDisposed: Boolean = false
        var isStopped: Boolean = false
        var locationEngineResolution: Resolution = locationEngineResolution
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var isTracking: Boolean = false
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val trackables: MutableSet<Trackable> = mutableSetOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val trackableStates: MutableMap<String, TrackableState> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val lastChannelConnectionStateChanges: MutableMap<String, ConnectionStateChange> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, null
        )
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val resolutions: MutableMap<String, Resolution> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val lastSentRawLocations: MutableMap<String, Location> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val skippedEnhancedLocations: SkippedLocations = SkippedLocations()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val skippedRawLocations: SkippedLocations = SkippedLocations()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var estimatedArrivalTimeInMilliseconds: Long? = null
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var lastPublisherLocation: Location? = null
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var currentDestination: Destination? = null
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val subscribers: MutableMap<String, MutableSet<Subscriber>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val requests: MutableMap<String, MutableMap<Subscriber, Resolution>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var presenceData: PresenceData = PresenceData(ClientTypes.PUBLISHER, rawLocations = areRawLocationsEnabled)
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        var active: Trackable? = null
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
            set(value) {
                this@DefaultCorePublisher.active = value
                field = value
            }
        var routingProfile: RoutingProfile = routingProfile
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
            set(value) {
                this@DefaultCorePublisher.routingProfile = value
                field = value
            }
        val rawLocationChangedCommands: MutableList<(State) -> Unit> = mutableListOf()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationChangedEvent> =
            LocationsPublishingState()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val rawLocationsPublishingState: LocationsPublishingState<RawLocationChangedEvent> = LocationsPublishingState()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field
        val duplicateTrackableGuard: DuplicateTrackableGuard = DuplicateTrackableGuard()
            get() = if (isDisposed) throw PublisherStateDisposedException() else field

        fun dispose() {
            trackables.clear()
            trackableStates.clear()
            trackableStateFlows.clear()
            lastChannelConnectionStateChanges.clear()
            resolutions.clear()
            lastSentEnhancedLocations.clear()
            lastSentRawLocations.clear()
            skippedEnhancedLocations.clearAll()
            skippedRawLocations.clearAll()
            estimatedArrivalTimeInMilliseconds = null
            active = null
            lastPublisherLocation = null
            currentDestination = null
            subscribers.clear()
            requests.clear()
            rawLocationChangedCommands.clear()
            enhancedLocationsPublishingState.clearAll()
            rawLocationsPublishingState.clearAll()
            duplicateTrackableGuard.clearAll()
            isDisposed = true
        }
    }
}
