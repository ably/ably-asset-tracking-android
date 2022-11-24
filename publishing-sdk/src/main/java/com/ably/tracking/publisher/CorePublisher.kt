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
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.TimeProvider
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.logging.w
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.guards.DefaultDuplicateTrackableGuard
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.guards.DefaultTrackableRemovalGuard
import com.ably.tracking.publisher.workerqueue.DefaultWorkerFactory
import com.ably.tracking.publisher.workerqueue.DefaultWorkerQueue
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.WorkerQueue
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal interface CorePublisher {
    fun trackTrackable(trackable: Trackable, callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>)
    fun addTrackable(trackable: Trackable, callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>)
    fun removeTrackable(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>)
    fun changeRoutingProfile(routingProfile: RoutingProfile)
    fun stop(timeoutInMilliseconds: Long, callbackFunction: ResultCallbackFunction<Unit>)
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
    fun closeMapbox()
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
    fun processRawLocationUpdate(
        rawLocationUpdate: LocationUpdate,
        properties: PublisherProperties,
        trackableId: String
    )

    fun processEnhancedLocationUpdate(
        enhancedLocationUpdate: EnhancedLocationUpdate,
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
    constantLocationEngineResolution: Resolution?,
): CorePublisher {
    return DefaultCorePublisher(
        ably,
        mapbox,
        resolutionPolicyFactory,
        routingProfile,
        logHandler,
        areRawLocationsEnabled,
        sendResolutionEnabled,
        constantLocationEngineResolution,
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
    private val constantLocationEngineResolution: Resolution?,
) : CorePublisher, TimeProvider {
    private val TAG = createLoggingTag(this)
    private val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
    private val workerQueue: WorkerQueue
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
        val properties = Properties(
            routingProfile,
            policy.resolve(emptySet()),
            constantLocationEngineResolution != null,
            areRawLocationsEnabled,
        )
        workerFactory = DefaultWorkerFactory(ably, hooks, this, policy, mapbox, this, logHandler)
        workerQueue = DefaultWorkerQueue(properties, scope, workerFactory)
        ably.subscribeForAblyStateChange { enqueue(workerFactory.createWorker(WorkerParams.AblyConnectionStateChange(it))) }
        mapbox.setLocationHistoryListener { historyData -> scope.launch { _locationHistory.emit(historyData) } }
    }

    private fun registerLocationObserver() {
        mapbox.registerLocationObserver(object : LocationUpdatesObserver {
            override fun onRawLocationChanged(rawLocation: Location) {
                logHandler?.v("$TAG Raw location received: $rawLocation")
                enqueue(workerFactory.createWorker(WorkerParams.RawLocationChanged(rawLocation)))
            }

            override fun onEnhancedLocationChanged(enhancedLocation: Location, intermediateLocations: List<Location>) {
                logHandler?.v("$TAG Enhanced location received: $enhancedLocation")
                val locationUpdateType =
                    if (intermediateLocations.isEmpty()) LocationUpdateType.ACTUAL else LocationUpdateType.PREDICTED
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.EnhancedLocationChanged(
                            enhancedLocation,
                            intermediateLocations,
                            locationUpdateType
                        )
                    )
                )
            }
        })
    }

    private fun enqueue(worker: Worker) {
        workerQueue.enqueue(worker)
    }

    override fun trackTrackable(
        trackable: Trackable,
        callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) {
        addTrackable(trackable) { addTrackableResult ->
            if (addTrackableResult.isSuccess) {
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.SetActiveTrackable(trackable) {
                            callbackFunction(addTrackableResult)
                        }
                    )
                )
            } else {
                callbackFunction(addTrackableResult)
            }
        }
    }

    override fun addTrackable(
        trackable: Trackable,
        callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) {
        enqueue(
            workerFactory.createWorker(
                WorkerParams.AddTrackable(
                    trackable = trackable,
                    callbackFunction = callbackFunction,
                    presenceUpdateListener = {
                        enqueue(workerFactory.createWorker(WorkerParams.PresenceMessage(trackable, it)))
                    },
                    channelStateChangeListener = {
                        enqueue(workerFactory.createWorker(WorkerParams.ChannelConnectionStateChange(trackable.id, it)))
                    },
                )
            )
        )
    }

    override fun removeTrackable(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>) {
        enqueue(workerFactory.createWorker(WorkerParams.RemoveTrackable(trackable, callbackFunction)))
    }

    override fun changeRoutingProfile(routingProfile: RoutingProfile) {
        enqueue(workerFactory.createWorker(WorkerParams.ChangeRoutingProfile(routingProfile)))
    }

    override fun stop(timeoutInMilliseconds: Long, callbackFunction: ResultCallbackFunction<Unit>) {
        enqueue(workerFactory.createWorker(WorkerParams.Stop(callbackFunction, timeoutInMilliseconds)))
    }

    override fun retrySendingEnhancedLocation(
        properties: PublisherProperties,
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate
    ) {
        logHandler?.v("$TAG Trackable $trackableId retry sending enhanced location ${locationUpdate.location}")
        properties.enhancedLocationsPublishingState.incrementRetryCount(trackableId)
        sendEnhancedLocationUpdate(
            locationUpdate,
            properties,
            trackableId
        )
    }

    override fun processEnhancedLocationUpdate(
        enhancedLocationUpdate: EnhancedLocationUpdate,
        properties: PublisherProperties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Processing enhanced location for trackable: $trackableId. ${enhancedLocationUpdate.location}")
        when {
            properties.enhancedLocationsPublishingState.hasPendingMessage(trackableId) -> {
                logHandler?.v("$TAG Trackable: $trackableId has pending message. Adding enhanced location to waiting ${enhancedLocationUpdate.location}")
                properties.enhancedLocationsPublishingState.addToWaiting(trackableId, enhancedLocationUpdate)
            }
            shouldSendLocation(
                enhancedLocationUpdate.location,
                properties.lastSentEnhancedLocations[trackableId],
                properties.resolutions[trackableId]
            ) -> {
                sendEnhancedLocationUpdate(enhancedLocationUpdate, properties, trackableId)
            }
            else -> {
                saveEnhancedLocationForFurtherSending(properties, trackableId, enhancedLocationUpdate.location)
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
        enhancedLocationUpdate: EnhancedLocationUpdate,
        properties: PublisherProperties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Trackable: $trackableId will send enhanced location ${enhancedLocationUpdate.location}")
        val locationUpdate = EnhancedLocationUpdate(
            enhancedLocationUpdate.location,
            properties.skippedEnhancedLocations.toList(trackableId),
            enhancedLocationUpdate.intermediateLocations,
            enhancedLocationUpdate.type
        )
        properties.enhancedLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendEnhancedLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.SendEnhancedLocationSuccess(
                            locationUpdate.location,
                            trackableId
                        )
                    )
                )
            } else {
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.SendEnhancedLocationFailure(
                            locationUpdate,
                            trackableId,
                            it.exceptionOrNull()
                        )
                    )
                )
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
        sendRawLocationUpdate(locationUpdate, properties, trackableId)
    }

    override fun processRawLocationUpdate(
        rawLocationUpdate: LocationUpdate,
        properties: PublisherProperties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Processing raw location for trackable: $trackableId. ${rawLocationUpdate.location}")
        when {
            properties.rawLocationsPublishingState.hasPendingMessage(trackableId) -> {
                logHandler?.v("$TAG Trackable: $trackableId has pending message. Adding raw location to waiting ${rawLocationUpdate.location}")
                properties.rawLocationsPublishingState.addToWaiting(trackableId, rawLocationUpdate)
            }
            shouldSendLocation(
                rawLocationUpdate.location,
                properties.lastSentRawLocations[trackableId],
                properties.resolutions[trackableId]
            ) -> {
                sendRawLocationUpdate(rawLocationUpdate, properties, trackableId)
            }
            else -> {
                saveRawLocationForFurtherSending(properties, trackableId, rawLocationUpdate.location)
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
        rawLocationUpdate: LocationUpdate,
        properties: PublisherProperties,
        trackableId: String
    ) {
        logHandler?.v("$TAG Trackable: $trackableId will send raw location ${rawLocationUpdate.location}")
        val locationUpdate = LocationUpdate(
            rawLocationUpdate.location,
            properties.skippedRawLocations.toList(trackableId),
        )
        properties.rawLocationsPublishingState.markMessageAsPending(trackableId)
        ably.sendRawLocation(trackableId, locationUpdate) {
            if (it.isSuccess) {
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.SendRawLocationSuccess(locationUpdate.location, trackableId)
                    )
                )
            } else {
                enqueue(
                    workerFactory.createWorker(
                        WorkerParams.SendRawLocationFailure(locationUpdate, trackableId, it.exceptionOrNull())
                    )
                )
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
        mapbox.stopTrip()
    }

    override fun closeMapbox() {
        mapbox.close()
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startLocationUpdates(properties: PublisherProperties) {
        properties.isTracking = true
        registerLocationObserver()
        mapbox.startTrip()
    }

    override fun updateTrackableState(properties: PublisherProperties, trackableId: String) {
        val hasSentAtLeastOneLocation: Boolean = properties.lastSentEnhancedLocations[trackableId] != null
        val lastChannelConnectionStateChange = getLastChannelConnectionStateChange(properties, trackableId)
        val isSubscribedToPresence = properties.trackableSubscribedToPresenceFlags[trackableId] == true
        val newTrackableState = when (properties.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE ->
                        when {
                            hasSentAtLeastOneLocation && isSubscribedToPresence -> TrackableState.Online
                            hasSentAtLeastOneLocation && !isSubscribedToPresence -> TrackableState.Publishing
                            else -> TrackableState.Offline()
                        }
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(properties.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newTrackableState != properties.trackableStates[trackableId]) {
            properties.trackableStates[trackableId] = newTrackableState
            scope.launch {
                if (properties.state != PublisherState.STOPPED) {
                    properties.trackableStateFlows[trackableId]?.emit(newTrackableState)
                }
            }
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
            if (properties.resolutions[trackable.id] != resolution) {
                properties.resolutions[trackable.id] = resolution
                enqueue(workerFactory.createWorker(WorkerParams.ChangeLocationEngineResolution))
                if (sendResolutionEnabled) {
                    // For now we ignore the result of this operation but perhaps we should retry it if it fails
                    ably.updatePresenceData(trackable.id, properties.presenceData.copy(resolution = resolution)) {}
                }
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
                        enqueue(workerFactory.createWorker(WorkerParams.DestinationSet(it.getOrThrow())))
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
            enqueue(workerFactory.createWorker(WorkerParams.RefreshResolutionPolicy))
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
        isLocationEngineResolutionConstant: Boolean,
        areRawLocationsEnabled: Boolean?,
    ) : PublisherProperties {
        private var isDisposed: Boolean = false
        override var locationEngineResolution: Resolution = locationEngineResolution
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val isLocationEngineResolutionConstant: Boolean = isLocationEngineResolutionConstant
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override var isTracking: Boolean = false
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackables: MutableSet<Trackable> = mutableSetOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableStates: MutableMap<String, TrackableState> = mutableMapOf()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableSubscribedToPresenceFlags: MutableMap<String, Boolean> = mutableMapOf()
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
        override val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationUpdate> =
            LocationsPublishingState()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val rawLocationsPublishingState: LocationsPublishingState<LocationUpdate> =
            LocationsPublishingState()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val duplicateTrackableGuard: DuplicateTrackableGuard = DefaultDuplicateTrackableGuard()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val trackableRemovalGuard: TrackableRemovalGuard = DefaultTrackableRemovalGuard()
            get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        override val areRawLocationsEnabled: Boolean = areRawLocationsEnabled ?: false
        override var state: PublisherState = PublisherState.IDLE
            set(value) {
                // Once we stop publisher it should never change its state
                if (field == PublisherState.STOPPED) {
                    throw PublisherStoppedException()
                }
                field = value
            }
        override val hasNoTrackablesAddingOrAdded: Boolean
            get() = trackables.isEmpty() && !duplicateTrackableGuard.isCurrentlyAddingAnyTrackable()

        override fun dispose() {
            trackables.clear()
            trackableStates.clear()
            trackableStateFlows.clear()
            trackableSubscribedToPresenceFlags.clear()
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
