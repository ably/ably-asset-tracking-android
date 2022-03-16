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
import com.ably.tracking.publisher.workerqueue.DefaultWorkerFactory
import com.ably.tracking.publisher.workerqueue.EventWorkerQueue
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.WorkerQueue
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
    fun processNextWaitingEnhancedLocationUpdate(properties: PublisherProperties, trackableId: String)
    fun saveEnhancedLocationForFurtherSending(properties: PublisherProperties, trackableId: String, location: Location)
    fun retrySendingEnhancedLocation(
        properties: PublisherProperties,
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate
    )

    fun updateLocations(locationUpdate: LocationUpdate)
    fun processNextWaitingRawLocationUpdate(properties: PublisherProperties, trackableId: String)
    fun retrySendingRawLocation(properties: PublisherProperties, trackableId: String, locationUpdate: LocationUpdate)
    fun saveRawLocationForFurtherSending(properties: PublisherProperties, trackableId: String, location: Location)
    fun processRawLocationUpdate(event: RawLocationChangedEvent, properties: PublisherProperties, trackableId: String)
    fun processEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        properties: PublisherProperties,
        trackableId: String
    )

    fun checkThreshold(
        currentLocation: Location,
        activeTrackable: Trackable?,
        estimatedArrivalTimeInMilliseconds: Long?
    )

    fun updateTrackables(properties: PublisherProperties)
    fun updateTrackableStateFlows(properties: PublisherProperties)
    fun updateTrackableState(properties: PublisherProperties, trackableId: String)
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
    areRawLocationsEnabled: Boolean?,
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
    private val workerFactory: WorkerFactory

    init {
        policy = resolutionPolicyFactory.createResolutionPolicy(
            hooks,
            methods
        )
        val channel = Channel<Event>()
        sendEventChannel = channel
        workerFactory = DefaultWorkerFactory(ably, hooks, this, policy, mapbox, this, logHandler)
        scope.launch {
            coroutineScope {
                sequenceEventsQueue(channel, routingProfile, areRawLocationsEnabled)
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
        routingProfile: RoutingProfile,
        areRawLocationsEnabled: Boolean?,
    ) {
        launch {
            val properties = Properties(routingProfile, policy.resolve(emptySet()), areRawLocationsEnabled)
            val workerQueue: WorkerQueue = EventWorkerQueue(this@DefaultCorePublisher, properties, scope, workerFactory)
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
                            workerFactory.createWorker(WorkerParams.DestinationSet(event.routeDurationInMilliseconds))
                        )
                    }
                    is RawLocationChangedEvent -> {
                        workerQueue.execute(workerFactory.createWorker(WorkerParams.RawLocationChanged(event.location)))
                    }
                    is EnhancedLocationChangedEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.EnhancedLocationChanged(
                                    event.location,
                                    event.intermediateLocations,
                                    event.type,
                                )
                            )
                        )
                    }
                    is TrackTrackableEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.AddTrackable(
                                    event.trackable,
                                    callbackFunction = { result ->
                                        if (result.isSuccess) {
                                            request(
                                                SetActiveTrackableEvent(event.trackable) { event.callbackFunction(result) }
                                            )
                                        } else {
                                            event.callbackFunction(result)
                                        }
                                    },
                                    presenceUpdateListener = {}, // temporary placeholder
                                    channelStateChangeListener = {}, // temporary placeholder
                                )
                            )
                        )
                    }
                    is SetActiveTrackableEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.SetActiveTrackable(
                                    event.trackable,
                                    event.callbackFunction
                                )
                            )
                        )
                    }
                    is AddTrackableEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.AddTrackable(
                                    event.trackable,
                                    event.callbackFunction,
                                    presenceUpdateListener = {}, // temporary placeholder
                                    channelStateChangeListener = {}, // temporary placeholder
                                )
                            )
                        )
                    }
                    is AddTrackableFailedEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.AddTrackableFailed(
                                    event.trackable,
                                    event.callbackFunction,
                                    event.exception
                                )
                            )
                        )
                    }
                    is PresenceMessageEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.PresenceMessage(
                                    event.trackable,
                                    event.presenceMessage,
                                )
                            )
                        )
                    }
                    is TrackableRemovalRequestedEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.TrackableRemovalRequested(
                                    event.trackable,
                                    event.callbackFunction,
                                    event.result
                                )
                            )
                        )
                    }
                    is ConnectionForTrackableCreatedEvent -> {
                        // for now delegate the presence listening to here, this could likely be a new case for new
                        // structure
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.ConnectionCreated(
                                    event.trackable,
                                    event.callbackFunction,
                                    presenceUpdateListener = { presenceMessage ->
                                        enqueue(PresenceMessageEvent(event.trackable, presenceMessage))
                                    },
                                    channelStateChangeListener = {}, // temporary placeholder
                                )
                            )
                        )
                    }
                    is ConnectionForTrackableReadyEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.ConnectionReady(
                                    event.trackable,
                                    event.callbackFunction,
                                ) { channelStateChange ->
                                    enqueue(ChannelConnectionStateChangeEvent(channelStateChange, event.trackable.id))
                                }
                            )
                        )
                    }
                    is ChangeLocationEngineResolutionEvent -> {
                        workerQueue.execute(workerFactory.createWorker(WorkerParams.ChangeLocationEngineResolution))
                    }
                    is RemoveTrackableEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.RemoveTrackable(
                                    event.trackable,
                                    event.callbackFunction
                                )
                            )
                        )
                    }
                    is DisconnectSuccessEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.DisconnectSuccess(
                                    event.trackable,
                                    event.callbackFunction,
                                    shouldRecalculateResolutionCallback = { enqueue(ChangeLocationEngineResolutionEvent) }
                                )
                            )
                        )
                    }
                    is RefreshResolutionPolicyEvent -> {
                        workerQueue.execute(workerFactory.createWorker(WorkerParams.RefreshResolutionPolicy))
                    }
                    is ChangeRoutingProfileEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.ChangeRoutingProfile(event.routingProfile)
                            )
                        )
                    }
                    is StopEvent -> {
                        workerQueue.execute(workerFactory.createWorker(WorkerParams.Stop(event.callbackFunction)))
                    }
                    is AblyConnectionStateChangeEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.AblyConnectionStateChange(event.connectionStateChange)
                            )
                        )
                    }
                    is ChannelConnectionStateChangeEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.ChannelConnectionStateChange(
                                    event.trackableId,
                                    event.connectionStateChange
                                )
                            )
                        )
                    }
                    is SendEnhancedLocationSuccessEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.SendEnhancedLocationSuccess(
                                    event.location,
                                    event.trackableId
                                )
                            )
                        )
                    }
                    is SendEnhancedLocationFailureEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.SendEnhancedLocationFailure(
                                    event.locationUpdate,
                                    event.trackableId,
                                    event.exception,
                                )
                            )
                        )
                    }
                    is SendRawLocationSuccessEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.SendRawLocationSuccess(
                                    event.location,
                                    event.trackableId,
                                )
                            )
                        )
                    }
                    is SendRawLocationFailureEvent -> {
                        workerQueue.execute(
                            workerFactory.createWorker(
                                WorkerParams.SendRawLocationFailure(
                                    event.locationUpdate,
                                    event.trackableId,
                                    event.exception,
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    override fun retrySendingEnhancedLocation(
        properties: PublisherProperties,
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

    override fun processEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        properties: PublisherProperties,
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

    override fun processNextWaitingEnhancedLocationUpdate(properties: PublisherProperties, trackableId: String) {
        properties.enhancedLocationsPublishingState.getNextWaiting(trackableId)?.let {
            logHandler?.v("$TAG Trackable: $trackableId. Process next waiting enhanced location ${it.location}")
            processEnhancedLocationUpdate(it, properties, trackableId)
        }
    }

    private fun sendEnhancedLocationUpdate(
        event: EnhancedLocationChangedEvent,
        properties: PublisherProperties,
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

    override fun saveEnhancedLocationForFurtherSending(
        properties: PublisherProperties,
        trackableId: String,
        location: Location
    ) {
        logHandler?.v("$TAG Trackable: $trackableId. Put enhanced location to the skippedLocations $location")
        properties.skippedEnhancedLocations.add(trackableId, location)
    }

    override fun retrySendingRawLocation(
        properties: PublisherProperties,
        trackableId: String,
        locationUpdate: LocationUpdate
    ) {
        logHandler?.v("$TAG Trackable $trackableId retry sending raw location ${locationUpdate.location}")
        properties.rawLocationsPublishingState.incrementRetryCount(trackableId)
        sendRawLocationUpdate(RawLocationChangedEvent(locationUpdate.location), properties, trackableId)
    }

    override fun processRawLocationUpdate(
        event: RawLocationChangedEvent,
        properties: PublisherProperties,
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

    override fun processNextWaitingRawLocationUpdate(properties: PublisherProperties, trackableId: String) {
        properties.rawLocationsPublishingState.getNextWaiting(trackableId)?.let {
            logHandler?.v("$TAG Trackable: $trackableId. Process next waiting raw location ${it.location}")
            processRawLocationUpdate(it, properties, trackableId)
        }
    }

    private fun sendRawLocationUpdate(
        event: RawLocationChangedEvent,
        properties: PublisherProperties,
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

    override fun saveRawLocationForFurtherSending(
        properties: PublisherProperties,
        trackableId: String,
        location: Location
    ) {
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

    override fun updateTrackableState(properties: PublisherProperties, trackableId: String) {
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
        properties: PublisherProperties,
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
            enqueue(ChangeLocationEngineResolutionEvent)
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

    override fun checkThreshold(
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

    override fun updateLocations(locationUpdate: LocationUpdate) {
        scope.launch { _locations.emit(locationUpdate) }
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
        override val rawLocationChangedCommands: MutableList<(PublisherProperties) -> Unit> = mutableListOf()
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
        override val areRawLocationsEnabled: Boolean = areRawLocationsEnabled ?: false

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
