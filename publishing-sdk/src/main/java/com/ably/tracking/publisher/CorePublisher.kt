package com.ably.tracking.publisher

import android.Manifest
import androidx.annotation.RequiresPermission
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
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.TimeProvider
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.logging.w
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.guards.DublicateTrackableGuardImpl
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuardImpl
import com.ably.tracking.publisher.workerqueue.EventWorkerQueue
import com.ably.tracking.publisher.workerqueue.WorkerQueue
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.ChangeLocationEngineResolutionWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionCreatedWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionReadyWorker
import com.ably.tracking.publisher.workerqueue.workers.DestinationSetWorker
import com.ably.tracking.publisher.workerqueue.workers.DisconnectSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.PresenceMessageWorker
import com.ably.tracking.publisher.workerqueue.workers.RemoveTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.SetActiveTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.StopWorker
import com.ably.tracking.publisher.workerqueue.workers.TrackableRemovalRequestedWorker
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

    fun addSubscriber(id: String, trackable: Trackable, data: PresenceData, properties: PublisherProperties)
    fun updateSubscriber(
        id: String,
        trackable: Trackable,
        data: PresenceData,
        properties: PublisherProperties
    )

    fun removeSubscriber(id: String, trackable: Trackable, properties: PublisherProperties)
    fun removeAllSubscribers(trackable: Trackable, properties: PublisherProperties)
    fun setDestination(destination: Destination, properties: PublisherProperties)
    fun removeCurrentDestination(properties: PublisherProperties)
    fun startLocationUpdates(properties: PublisherProperties)
    fun stopLocationUpdates(properties: PublisherProperties)

    fun updateTrackables(properties: PublisherProperties)
    fun updateTrackableStateFlows(properties: PublisherProperties)
    fun notifyResolutionPolicyThatTrackableWasRemoved(trackable: Trackable)
    fun notifyResolutionPolicyThatActiveTrackableHasChanged(trackable: Trackable?)
    fun resolveResolution(trackable: Trackable, properties: PublisherProperties)
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

internal class DefaultCorePublisher
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
constructor(
    private val ably: Ably,
    private val mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    override var routingProfile: RoutingProfile,
    private val logHandler: LogHandler?,
    private val areRawLocationsEnabled: Boolean?,
    private val sendResolutionEnabled: Boolean,
) : CorePublisher, TimeProvider {
    private val TAG = createLoggingTag(this)
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
                logHandler?.v("$TAG Raw location received: $rawLocation")
                enqueue(RawLocationChangedEvent(rawLocation))
            }

            override fun onEnhancedLocationChanged(enhancedLocation: Location, intermediateLocations: List<Location>) {
                logHandler?.v("$TAG Enhanced location received: $enhancedLocation")
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
            val properties = Properties(routingProfile, policy.resolve(emptySet()), areRawLocationsEnabled)
            val workerQueue: WorkerQueue = EventWorkerQueue(this@DefaultCorePublisher, properties, scope)
            // processing
            for (event in receiveEventChannel) {
                // handle events after the publisher is stopped
                if (properties.isStopped) {
                    if (event is Request<*>) {
                        // when the event is a request then call its handler
                        when (event) {
                            is StopEvent -> event.callbackFunction(Result.success(Unit))
                            else -> event.callbackFunction(Result.failure(PublisherStoppedException()))
                        }
                        continue
                    } else if (event is AdhocEvent) {
                        // when the event is an adhoc event then just ignore it
                        continue
                    }
                }
                when (event) {
                    is SetDestinationSuccessEvent -> {
                        workerQueue.execute(
                            DestinationSetWorker(
                                event.routeDurationInMilliseconds,
                                this@DefaultCorePublisher
                            )
                        )
                    }
                    is RawLocationChangedEvent -> {
                        logHandler?.v("$TAG Raw location changed event received ${event.location}")
                        properties.lastPublisherLocation = event.location
                        if (areRawLocationsEnabled == true) {
                            properties.trackables.forEach { processRawLocationUpdate(event, properties, it.id) }
                        }
                        properties.rawLocationChangedCommands.apply {
                            if (isNotEmpty()) {
                                forEach { command -> command(properties) }
                                clear()
                            }
                        }
                    }
                    is EnhancedLocationChangedEvent -> {
                        logHandler?.v("$TAG Enhanced location changed event received ${event.location}")
                        properties.trackables.forEach { processEnhancedLocationUpdate(event, properties, it.id) }
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
                            properties.active,
                            properties.estimatedArrivalTimeInMilliseconds
                        )
                    }
                    is TrackTrackableEvent -> {
                        request(
                            AddTrackableEvent(event.trackable) { result ->
                                if (result.isSuccess) {
                                    request(SetActiveTrackableEvent(event.trackable) { event.callbackFunction(result) })
                                } else {
                                    event.callbackFunction(result)
                                }
                            }
                        )
                    }
                    is SetActiveTrackableEvent -> {
                        workerQueue.execute(
                            SetActiveTrackableWorker(
                                event.trackable, event.callbackFunction,
                                this@DefaultCorePublisher, hooks
                            )
                        )
                    }
                    is AddTrackableEvent -> {
                        workerQueue.execute(
                            AddTrackableWorker(event.trackable, event.callbackFunction, ably)
                        )
                    }
                    is AddTrackableFailedEvent -> {
                        workerQueue.execute(
                            AddTrackableFailedWorker(
                                event.trackable,
                                event.callbackFunction,
                                event.exception
                            )
                        )
                    }
                    is PresenceMessageEvent -> {
                        workerQueue.execute(
                            PresenceMessageWorker(
                                event.trackable,
                                event.presenceMessage,
                                this@DefaultCorePublisher
                            )
                        )
                    }
                    is TrackableRemovalRequestedEvent -> {
                        workerQueue.execute(
                            TrackableRemovalRequestedWorker(
                                event.trackable, event.callbackFunction,
                                event.result
                            )
                        )
                    }
                    is ConnectionForTrackableCreatedEvent -> {
                        // for now delegate the presence listening to here, this could likely be a new case for new
                        // structure
                        workerQueue.execute(
                            ConnectionCreatedWorker(
                                event.trackable,
                                event.callbackFunction,
                                ably
                            ) { presenceMessage ->
                                enqueue(PresenceMessageEvent(event.trackable, presenceMessage))
                            }
                        )
                    }
                    is ConnectionForTrackableReadyEvent -> {
                        workerQueue.execute(
                            ConnectionReadyWorker(
                                event.trackable,
                                event.callbackFunction,
                                ably,
                                hooks,
                                this@DefaultCorePublisher
                            ) { channelStateChange ->
                                enqueue(ChannelConnectionStateChangeEvent(channelStateChange, event.trackable.id))
                            }
                        )
                    }
                    is ChangeLocationEngineResolutionEvent -> {
                        workerQueue.execute(ChangeLocationEngineResolutionWorker(policy, mapbox))
                    }
                    is RemoveTrackableEvent -> {
                        workerQueue.execute(RemoveTrackableWorker(event.trackable, event.callbackFunction, ably))
                    }
                    is DisconnectSuccessEvent -> {
                        workerQueue.execute(
                            DisconnectSuccessWorker(
                                event.trackable,
                                event.callbackFunction,
                                this@DefaultCorePublisher,
                            )
                        )
                    }
                    is RefreshResolutionPolicyEvent -> {
                        properties.trackables.forEach { resolveResolution(it, properties) }
                    }
                    is ChangeRoutingProfileEvent -> {
                        properties.routingProfile = event.routingProfile
                        properties.currentDestination?.let { setDestination(it, properties) }
                    }
                    is StopEvent -> {
                        workerQueue.execute(StopWorker(event.callbackFunction, ably, this@DefaultCorePublisher))
                    }
                    is AblyConnectionStateChangeEvent -> {
                        logHandler?.v("$TAG Ably connection state changed ${event.connectionStateChange.state}")
                        properties.lastConnectionStateChange = event.connectionStateChange
                        properties.trackables.forEach {
                            updateTrackableState(properties, it.id)
                        }
                    }
                    is ChannelConnectionStateChangeEvent -> {
                        logHandler?.v("$TAG Trackable ${event.trackableId} connection state changed ${event.connectionStateChange.state}")
                        properties.lastChannelConnectionStateChanges[event.trackableId] = event.connectionStateChange
                        updateTrackableState(properties, event.trackableId)
                    }
                    is SendEnhancedLocationSuccessEvent -> {
                        logHandler?.v("$TAG Trackable ${event.trackableId} successfully sent enhanced location ${event.location}")
                        properties.enhancedLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                        properties.lastSentEnhancedLocations[event.trackableId] = event.location
                        properties.skippedEnhancedLocations.clear(event.trackableId)
                        updateTrackableState(properties, event.trackableId)
                        processNextWaitingEnhancedLocationUpdate(properties, event.trackableId)
                    }
                    is SendEnhancedLocationFailureEvent -> {
                        logHandler?.w(
                            "$TAG Trackable ${event.trackableId} failed to send enhanced location ${event.locationUpdate.location}",
                            event.exception
                        )
                        if (properties.enhancedLocationsPublishingState.shouldRetryPublishing(event.trackableId)) {
                            retrySendingEnhancedLocation(properties, event.trackableId, event.locationUpdate)
                        } else {
                            properties.enhancedLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                            saveEnhancedLocationForFurtherSending(
                                properties,
                                event.trackableId,
                                event.locationUpdate.location
                            )
                            processNextWaitingEnhancedLocationUpdate(properties, event.trackableId)
                        }
                    }
                    is SendRawLocationSuccessEvent -> {
                        logHandler?.v("$TAG Trackable ${event.trackableId} successfully sent raw location ${event.location}")
                        properties.rawLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                        properties.lastSentRawLocations[event.trackableId] = event.location
                        properties.skippedRawLocations.clear(event.trackableId)
                        processNextWaitingRawLocationUpdate(properties, event.trackableId)
                    }
                    is SendRawLocationFailureEvent -> {
                        logHandler?.w(
                            "$TAG Trackable ${event.trackableId} failed to send raw location ${event.locationUpdate.location}",
                            event.exception
                        )
                        if (properties.rawLocationsPublishingState.shouldRetryPublishing(event.trackableId)) {
                            retrySendingRawLocation(properties, event.trackableId, event.locationUpdate)
                        } else {
                            properties.rawLocationsPublishingState.unmarkMessageAsPending(event.trackableId)
                            saveRawLocationForFurtherSending(
                                properties,
                                event.trackableId,
                                event.locationUpdate.location
                            )
                            processNextWaitingRawLocationUpdate(properties, event.trackableId)
                        }
                    }
                }
            }
        }
    }

    private fun retrySendingEnhancedLocation(
        properties: Properties,
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate
    ) {
        logHandler?.v("$TAG Trackable $trackableId retry sending enhanced location ${locationUpdate.location}")
        properties.enhancedLocationsPublishingState.incrementRetryCount(trackableId)
        sendEnhancedLocationUpdate(
            EnhancedLocationChangedEvent(
                locationUpdate.location,
                locationUpdate.intermediateLocations,
                locationUpdate.type
            ),
            properties,
            trackableId
        )
    }

    private fun processEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        properties: Properties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Processing enhanced location for trackable: $trackableId. ${event.location}")
        when {
            properties.enhancedLocationsPublishingState.hasPendingMessage(trackableId) -> {
                logHandler?.v("$TAG Trackable: $trackableId has pending message. Adding enhanced location to waiting ${event.location}")
                properties.enhancedLocationsPublishingState.addToWaiting(trackableId, event)
            }
            shouldSendLocation(
                event.location,
                properties.lastSentEnhancedLocations[trackableId],
                properties.resolutions[trackableId]
            ) -> {
                sendEnhancedLocationUpdate(event, properties, trackableId)
            }
            else -> {
                saveEnhancedLocationForFurtherSending(properties, trackableId, event.location)
                processNextWaitingEnhancedLocationUpdate(properties, trackableId)
            }
        }
    }

    private fun processNextWaitingEnhancedLocationUpdate(properties: Properties, trackableId: String) {
        properties.enhancedLocationsPublishingState.getNextWaiting(trackableId)?.let {
            logHandler?.v("$TAG Trackable: $trackableId. Process next waiting enhanced location ${it.location}")
            processEnhancedLocationUpdate(it, properties, trackableId)
        }
    }

    private fun sendEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        properties: Properties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Trackable: $trackableId will send enhanced location ${event.location}")
        val locationUpdate = EnhancedLocationUpdate(
            event.location,
            properties.skippedEnhancedLocations.toList(trackableId),
            event.intermediateLocations,
            event.type
        )
        properties.enhancedLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendEnhancedLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(SendEnhancedLocationSuccessEvent(locationUpdate.location, trackableId))
            } else {
                enqueue(SendEnhancedLocationFailureEvent(locationUpdate, trackableId, it.exceptionOrNull()))
            }
        }
    }

    private fun saveEnhancedLocationForFurtherSending(properties: Properties, trackableId: String, location: Location) {
        logHandler?.v("$TAG Trackable: $trackableId. Put enhanced location to the skippedLocations $location")
        properties.skippedEnhancedLocations.add(trackableId, location)
    }

    private fun retrySendingRawLocation(properties: Properties, trackableId: String, locationUpdate: LocationUpdate) {
        logHandler?.v("$TAG Trackable $trackableId retry sending raw location ${locationUpdate.location}")
        properties.rawLocationsPublishingState.incrementRetryCount(trackableId)
        sendRawLocationUpdate(RawLocationChangedEvent(locationUpdate.location), properties, trackableId)
    }

    private fun processRawLocationUpdate(
        event: RawLocationChangedEvent,
        properties: Properties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Processing raw location for trackable: $trackableId. ${event.location}")
        when {
            properties.rawLocationsPublishingState.hasPendingMessage(trackableId) -> {
                logHandler?.v("$TAG Trackable: $trackableId has pending message. Adding raw location to waiting ${event.location}")
                properties.rawLocationsPublishingState.addToWaiting(trackableId, event)
            }
            shouldSendLocation(
                event.location,
                properties.lastSentRawLocations[trackableId],
                properties.resolutions[trackableId]
            ) -> {
                sendRawLocationUpdate(event, properties, trackableId)
            }
            else -> {
                saveRawLocationForFurtherSending(properties, trackableId, event.location)
                processNextWaitingRawLocationUpdate(properties, trackableId)
            }
        }
    }

    private fun processNextWaitingRawLocationUpdate(properties: Properties, trackableId: String) {
        properties.rawLocationsPublishingState.getNextWaiting(trackableId)?.let {
            logHandler?.v("$TAG Trackable: $trackableId. Process next waiting raw location ${it.location}")
            processRawLocationUpdate(it, properties, trackableId)
        }
    }

    private fun sendRawLocationUpdate(
        event: RawLocationChangedEvent,
        properties: Properties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Trackable: $trackableId will send raw location ${event.location}")
        val locationUpdate = LocationUpdate(
            event.location,
            properties.skippedRawLocations.toList(trackableId),
        )
        properties.rawLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendRawLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(SendRawLocationSuccessEvent(locationUpdate.location, trackableId))
            } else {
                enqueue(SendRawLocationFailureEvent(locationUpdate, trackableId, it.exceptionOrNull()))
            }
        }
    }

    private fun saveRawLocationForFurtherSending(properties: Properties, trackableId: String, location: Location) {
        logHandler?.v("$TAG Trackable: $trackableId. Put raw location to the skippedLocations $location")
        properties.skippedRawLocations.add(trackableId, location)
    }

    override fun stopLocationUpdates(properties: PublisherProperties) {
        properties.isTracking = false
        mapbox.unregisterLocationObserver()
        mapbox.stopAndClose()
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startLocationUpdates(properties: PublisherProperties) {
        properties.isTracking = true
        mapbox.startTrip()
    }

    private fun updateTrackableState(properties: Properties, trackableId: String) {
        val hasSentAtLeastOneLocation: Boolean = properties.lastSentEnhancedLocations[trackableId] != null
        val lastChannelConnectionStateChange = getLastChannelConnectionStateChange(properties, trackableId)
        val newTrackableState = when (properties.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE -> if (hasSentAtLeastOneLocation) TrackableState.Online else TrackableState.Offline()
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(properties.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newTrackableState != properties.trackableStates[trackableId]) {
            properties.trackableStates[trackableId] = newTrackableState
            scope.launch { properties.trackableStateFlows[trackableId]?.emit(newTrackableState) }
        }
    }

    private fun getLastChannelConnectionStateChange(
        properties: Properties,
        trackableId: String
    ): ConnectionStateChange =
        properties.lastChannelConnectionStateChanges[trackableId]
            ?: ConnectionStateChange(ConnectionState.OFFLINE, null)

    override fun removeAllSubscribers(trackable: Trackable, properties: PublisherProperties) {
        properties.subscribers[trackable.id]?.let { subscribers ->
            subscribers.forEach { hooks.subscribers?.onSubscriberRemoved(it) }
            subscribers.clear()
        }
    }

    override fun addSubscriber(id: String, trackable: Trackable, data: PresenceData, properties: PublisherProperties) {
        val subscriber = Subscriber(id, trackable)
        if (properties.subscribers[trackable.id] == null) {
            properties.subscribers[trackable.id] = mutableSetOf()
        }
        properties.subscribers[trackable.id]?.add(subscriber)
        saveOrRemoveResolutionRequest(data.resolution, trackable, subscriber, properties)
        hooks.subscribers?.onSubscriberAdded(subscriber)
        resolveResolution(trackable, properties)
    }

    override fun updateSubscriber(
        id: String,
        trackable: Trackable,
        data: PresenceData,
        properties: PublisherProperties
    ) {
        properties.subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                data.resolution.let { resolution ->
                    saveOrRemoveResolutionRequest(resolution, trackable, subscriber, properties)
                    resolveResolution(trackable, properties)
                }
            }
        }
    }

    override fun removeSubscriber(id: String, trackable: Trackable, properties: PublisherProperties) {
        properties.subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                subscribers.remove(subscriber)
                properties.requests[trackable.id]?.remove(subscriber)
                hooks.subscribers?.onSubscriberRemoved(subscriber)
                resolveResolution(trackable, properties)
            }
        }
    }

    private fun saveOrRemoveResolutionRequest(
        resolution: Resolution?,
        trackable: Trackable,
        subscriber: Subscriber,
        properties: PublisherProperties
    ) {
        if (resolution != null) {
            if (properties.requests[trackable.id] == null) {
                properties.requests[trackable.id] = mutableMapOf()
            }
            properties.requests[trackable.id]?.put(subscriber, resolution)
        } else {
            properties.requests[trackable.id]?.remove(subscriber)
        }
    }

    override fun notifyResolutionPolicyThatTrackableWasRemoved(trackable: Trackable) {
        hooks.trackables?.onTrackableRemoved(trackable)
    }

    override fun notifyResolutionPolicyThatActiveTrackableHasChanged(trackable: Trackable?) {
        hooks.trackables?.onActiveTrackableChanged(trackable)
    }

    override fun resolveResolution(trackable: Trackable, properties: PublisherProperties) {
        val resolutionRequests: Set<Resolution> = properties.requests[trackable.id]?.values?.toSet() ?: emptySet()
        policy.resolve(TrackableResolutionRequest(trackable, resolutionRequests)).let { resolution ->
            properties.resolutions[trackable.id] = resolution
            enqueue(ChangeLocationEngineResolutionEvent())
            if (sendResolutionEnabled) {
                ably.sendResolution(trackable.id, resolution) {} // we ignore the result of this operation
            }
        }
    }

    override fun setDestination(destination: Destination, properties: PublisherProperties) {
        properties.lastPublisherLocation.let { currentLocation ->
            if (currentLocation != null) {
                removeCurrentDestination(properties)
                properties.currentDestination = destination
                mapbox.setRoute(currentLocation, destination, properties.routingProfile) {
                    try {
                        enqueue(SetDestinationSuccessEvent(it.getOrThrow()))
                    } catch (exception: MapException) {
                        logHandler?.w("Setting trackable destination failed", exception)
                    }
                }
            } else {
                properties.rawLocationChangedCommands.add { updatedProperties ->
                    setDestination(destination, updatedProperties)
                }
            }
        }
    }

    override fun removeCurrentDestination(properties: PublisherProperties) {
        mapbox.clearRoute()
        properties.currentDestination = null
        properties.estimatedArrivalTimeInMilliseconds = null
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
            return distanceFromLastSentLocation >= resolution.minimumDisplacement || timeSinceLastSentLocation >= resolution.desiredInterval
        } else {
            true
        }
    }

    override fun updateTrackables(properties: PublisherProperties) {
        scope.launch { _trackables.emit(properties.trackables) }
    }

    override fun updateTrackableStateFlows(properties: PublisherProperties) {
        trackableStateFlows = properties.trackableStateFlows
    }

    override fun getCurrentTimeInMilliseconds(): Long = System.currentTimeMillis()

    internal inner class Hooks : ResolutionPolicy.Hooks {
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

    internal inner class Properties(
        routingProfile: RoutingProfile,
        locationEngineResolution: Resolution,
        areRawLocationsEnabled: Boolean?,
    ) : PublisherProperties {
        private var isDisposed: Boolean = false
        override var isStopped: Boolean = false
        override var locationEngineResolution: Resolution = locationEngineResolution
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var isTracking: Boolean = false
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackables: MutableSet<Trackable> = mutableSetOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableStates: MutableMap<String, TrackableState> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val lastChannelConnectionStateChanges: MutableMap<String, ConnectionStateChange> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, null
        )
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val resolutions: MutableMap<String, Resolution> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val lastSentRawLocations: MutableMap<String, Location> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val skippedEnhancedLocations: SkippedLocations = SkippedLocations()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val skippedRawLocations: SkippedLocations = SkippedLocations()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var estimatedArrivalTimeInMilliseconds: Long? = null
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var lastPublisherLocation: Location? = null
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var currentDestination: Destination? = null
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val subscribers: MutableMap<String, MutableSet<Subscriber>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val requests: MutableMap<String, MutableMap<Subscriber, Resolution>> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var presenceData: PresenceData =
            PresenceData(ClientTypes.PUBLISHER, rawLocations = areRawLocationsEnabled)
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var active: Trackable? = null
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
            set(value) {
                this@DefaultCorePublisher.active = value
                field = value
            }
        override var routingProfile: RoutingProfile = routingProfile
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
            set(value) {
                this@DefaultCorePublisher.routingProfile = value
                field = value
            }
        override val rawLocationChangedCommands: MutableList<(Properties) -> Unit> = mutableListOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationChangedEvent> =
            LocationsPublishingState()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val rawLocationsPublishingState: LocationsPublishingState<RawLocationChangedEvent> =
            LocationsPublishingState()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val duplicateTrackableGuard: DuplicateTrackableGuard = DublicateTrackableGuardImpl()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableRemovalGuard: TrackableRemovalGuard = TrackableRemovalGuardImpl()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field

        override fun dispose() {
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
            trackableRemovalGuard.clearAll()
            isDisposed = true
        }
    }
}
