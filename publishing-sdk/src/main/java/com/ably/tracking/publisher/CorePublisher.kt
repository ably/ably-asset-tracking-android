package com.ably.tracking.publisher

import android.Manifest
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.AblyException
import com.ably.tracking.AssetStatus
import com.ably.tracking.ConnectionState
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.mapbox.navigation.core.trip.session.LocationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    fun request(request: Request)
    val locations: SharedFlow<LocationUpdate>
    val trackables: SharedFlow<Set<Trackable>>
    val locationHistory: SharedFlow<LocationHistoryData>
    val active: Trackable?
    val routingProfile: RoutingProfile
    val assetStatusFlows: Map<String, StateFlow<AssetStatus>>
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
internal fun createCorePublisher(
    ably: Ably,
    mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile,
    batteryDataProvider: BatteryDataProvider
): CorePublisher {
    return DefaultCorePublisher(ably, mapbox, resolutionPolicyFactory, routingProfile, batteryDataProvider)
}

private class DefaultCorePublisher
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
constructor(
    private val ably: Ably,
    private val mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile,
    private val batteryDataProvider: BatteryDataProvider
) : CorePublisher {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _locations = MutableSharedFlow<LocationUpdate>(replay = 1)
    private val _trackables = MutableSharedFlow<Set<Trackable>>(replay = 1)
    private val _locationHistory = MutableSharedFlow<LocationHistoryData>()
    private val thresholdChecker = ThresholdChecker()
    private val policy: ResolutionPolicy
    private val hooks = Hooks()
    private val methods = Methods()
    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            enqueue(
                RawLocationChangedEvent(
                    LocationUpdate(
                        rawLocation,
                        batteryDataProvider.getCurrentBatteryPercentage()
                    )
                )
            )
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            val intermediateLocations =
                if (keyPoints.size > 1) keyPoints.subList(0, keyPoints.size - 1) else emptyList()
            enqueue(
                EnhancedLocationChangedEvent(
                    EnhancedLocationUpdate(
                        enhancedLocation,
                        batteryDataProvider.getCurrentBatteryPercentage(),
                        intermediateLocations,
                        if (intermediateLocations.isEmpty()) LocationUpdateType.ACTUAL else LocationUpdateType.PREDICTED
                    )
                )
            )
        }
    }
    override val locations: SharedFlow<LocationUpdate>
        get() = _locations.asSharedFlow()
    override val trackables: SharedFlow<Set<Trackable>>
        get() = _trackables.asSharedFlow()
    override val locationHistory: SharedFlow<LocationHistoryData>
        get() = _locationHistory.asSharedFlow()

    override var active: Trackable? = null
    override var routingProfile: RoutingProfile = routingProfile
    override var assetStatusFlows: Map<String, StateFlow<AssetStatus>> = emptyMap()

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
        mapbox.registerLocationObserver(locationObserver)
        mapbox.setLocationHistoryListener { historyData -> scope.launch { _locationHistory.emit(historyData) } }
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request) {
        scope.launch { sendEventChannel.send(request) }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun CoroutineScope.sequenceEventsQueue(
        receiveEventChannel: ReceiveChannel<Event>,
        routingProfile: RoutingProfile
    ) {
        launch {
            // state
            val state = PublisherState(routingProfile, policy.resolve(emptySet()))

            // processing
            for (event in receiveEventChannel) {
                when (event) {
                    is StartEvent -> {
                        if (!state.isTracking) {
                            state.isTracking = true
                            mapbox.startTrip()
                        }
                    }
                    is SetDestinationSuccessEvent -> {
                        state.estimatedArrivalTimeInMilliseconds =
                            System.currentTimeMillis() + event.routeDurationInMilliseconds
                    }
                    is RawLocationChangedEvent -> {
                        state.lastPublisherLocation = event.locationUpdate.location
                        state.destinationToSet?.let { setDestination(it, state) }
                    }
                    is EnhancedLocationChangedEvent -> {
                        for (trackable in state.trackables) {
                            if (shouldSendLocation(
                                    event.locationUpdate.location,
                                    state.lastSentEnhancedLocations[trackable.id],
                                    state.resolutions[trackable.id]
                                )
                            ) {
                                try {
                                    ably.sendEnhancedLocation(trackable.id, event.locationUpdate)
                                    state.lastSentEnhancedLocations[trackable.id] = event.locationUpdate.location
                                    updateAssetStatuses(state, trackable.id)
                                } catch (exception: AblyException) {
                                    // TODO - what to do here if sending enhanced location fails?
                                }
                            }
                        }
                        scope.launch { _locations.emit(event.locationUpdate) }
                        checkThreshold(
                            event.locationUpdate.location,
                            state.active,
                            state.estimatedArrivalTimeInMilliseconds
                        )
                    }
                    is TrackTrackableEvent -> {
                        createChannelForTrackableIfNotExisits(
                            event.trackable,
                            { result ->
                                if (result.isSuccess) {
                                    request(SetActiveTrackableEvent(event.trackable) { event.handler(result) })
                                } else {
                                    event.handler(result)
                                }
                            },
                            state
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
                        createChannelForTrackableIfNotExisits(event.trackable, event.handler, state)
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
                    is JoinPresenceSuccessEvent -> {
                        state.trackables.add(event.trackable)
                        scope.launch { _trackables.emit(state.trackables) }
                        resolveResolution(event.trackable, state)
                        hooks.trackables?.onTrackableAdded(event.trackable)
                        val assetStatus = state.assetStatuses[event.trackable.id] ?: AssetStatus.Offline()
                        val assetStatusFlow =
                            state.assetStatusFlows[event.trackable.id] ?: MutableStateFlow(assetStatus)
                        state.assetStatusFlows[event.trackable.id] = assetStatusFlow
                        assetStatusFlows = state.assetStatusFlows
                        state.assetStatuses[event.trackable.id] = assetStatus
                        event.handler(Result.success(assetStatusFlow.asStateFlow()))
                    }
                    is ChangeLocationEngineResolutionEvent -> {
                        state.locationEngineResolution = policy.resolve(state.resolutions.values.toSet())
                        mapbox.changeResolution(state.locationEngineResolution)
                    }
                    is RemoveTrackableEvent -> {
                        val wasTrackablePresent = state.trackables.remove(event.trackable)
                        scope.launch { _trackables.emit(state.trackables) }
                        if (wasTrackablePresent) {
                            state.assetStatusFlows.remove(event.trackable.id) // there is no way to stop the StateFlow so we just remove it
                            assetStatusFlows = state.assetStatusFlows
                            state.assetStatuses.remove(event.trackable.id)
                            hooks.trackables?.onTrackableRemoved(event.trackable)
                            removeAllSubscribers(event.trackable, state)
                            state.resolutions.remove(event.trackable.id)
                                ?.let { enqueue(ChangeLocationEngineResolutionEvent()) }
                            state.requests.remove(event.trackable.id)
                            state.lastSentEnhancedLocations.remove(event.trackable.id)

                            // If this was the active Trackable then clear that state and remove destination.
                            if (state.active == event.trackable) {
                                removeCurrentDestination(state)
                                state.active = null
                                hooks.trackables?.onActiveTrackableChanged(null)
                            }

                            // Leave Ably channel.
                            ably.disconnect(event.trackable.id, state.presenceData) {
                                if (it.isSuccess) {
                                    event.handler(Result.success(true))
                                } else {
                                    event.handler(Result.failure(it.exceptionOrNull()!!))
                                }
                            }
                        } else {
                            // notify with false to indicate that it was not removed
                            event.handler(Result.success(false))
                        }
                    }
                    is RefreshResolutionPolicyEvent -> {
                        state.trackables.forEach { resolveResolution(it, state) }
                    }
                    is ChangeRoutingProfileEvent -> {
                        state.routingProfile = event.routingProfile
                        state.currentDestination?.let { setDestination(it, state) }
                    }
                    is StopEvent -> {
                        ably.close(state.presenceData)
                        if (state.isTracking) {
                            state.isTracking = false
                            mapbox.unregisterLocationObserver(locationObserver)
                            mapbox.stopAndClose()
                        }
                        // TODO implement proper stopping strategy which only calls back once we're fully stopped (considering whether scope.cancel() is appropriate)
                        event.handler(Result.success(Unit))
                    }
                    is AblyConnectionStateChangeEvent -> {
                        state.lastConnectionStateChange = event.connectionStateChange
                        state.trackables.forEach {
                            updateAssetStatuses(state, it.id)
                        }
                    }
                }
            }
        }
    }

    private fun updateAssetStatuses(state: PublisherState, trackableId: String) {
        val hasSentAtLeastOneLocation: Boolean = state.lastSentEnhancedLocations[trackableId] != null
        val newStatus = when (state.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> if (hasSentAtLeastOneLocation) AssetStatus.Online() else AssetStatus.Offline()
            ConnectionState.OFFLINE -> AssetStatus.Offline()
            ConnectionState.FAILED -> AssetStatus.Failed(state.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newStatus != state.assetStatuses[trackableId]) {
            state.assetStatuses[trackableId] = newStatus
            scope.launch { state.assetStatusFlows[trackableId]?.emit(newStatus) }
        }
    }

    private fun removeAllSubscribers(trackable: Trackable, state: PublisherState) {
        state.subscribers[trackable.id]?.let { subscribers ->
            subscribers.forEach { hooks.subscribers?.onSubscriberRemoved(it) }
            subscribers.clear()
        }
    }

    /**
     * Creates a [Channel] for the [Trackable], joins the channel's presence and enqueues [SuccessEvent].
     * If a [Channel] for the given [Trackable] exists then it just enqueues [SuccessEvent].
     * If during channel creation and joining presence an error occurs then it enqueues [FailureEvent] with the [AblyException].
     */
    private fun createChannelForTrackableIfNotExisits(
        trackable: Trackable,
        handler: ResultHandler<StateFlow<AssetStatus>>,
        state: PublisherState
    ) {
        ably.connect(trackable.id, state.presenceData) { result ->
            try {
                result.getOrThrow()
                try {
                    ably.subscribeForPresenceMessages(trackable.id) { enqueue(PresenceMessageEvent(trackable, it)) }
                } catch (exception: AblyException) {
                    // TODO - what to do here? should we fail the whole process when subscribing for presence fails? or should it continue?
                }
                request(JoinPresenceSuccessEvent(trackable, handler))
            } catch (exception: Exception) {
                handler(Result.failure(exception))
            }
        }
    }

    private fun addSubscriber(id: String, trackable: Trackable, data: PresenceData, state: PublisherState) {
        val subscriber = Subscriber(id, trackable)
        if (state.subscribers[trackable.id] == null) {
            state.subscribers[trackable.id] = mutableSetOf()
        }
        state.subscribers[trackable.id]?.add(subscriber)
        saveOrRemoveResolutionRequest(data.resolution, trackable, subscriber, state)
        hooks.subscribers?.onSubscriberAdded(subscriber)
        resolveResolution(trackable, state)
    }

    private fun updateSubscriber(id: String, trackable: Trackable, data: PresenceData, state: PublisherState) {
        state.subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                data.resolution.let { resolution ->
                    saveOrRemoveResolutionRequest(resolution, trackable, subscriber, state)
                    resolveResolution(trackable, state)
                }
            }
        }
    }

    private fun removeSubscriber(id: String, trackable: Trackable, state: PublisherState) {
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
        state: PublisherState
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

    private fun resolveResolution(trackable: Trackable, state: PublisherState) {
        val resolutionRequests: Set<Resolution> = state.requests[trackable.id]?.values?.toSet() ?: emptySet()
        policy.resolve(TrackableResolutionRequest(trackable, resolutionRequests)).let { resolution ->
            state.resolutions[trackable.id] = resolution
            enqueue(ChangeLocationEngineResolutionEvent())
        }
    }

    private fun setDestination(destination: Destination, state: PublisherState) {
        // TODO is there a way to ensure we're executing in the right thread?
        state.lastPublisherLocation.let { currentLocation ->
            if (currentLocation != null) {
                state.destinationToSet = null
                removeCurrentDestination(state)
                state.currentDestination = destination
                mapbox.setRoute(currentLocation, destination, state.routingProfile) {
                    enqueue(SetDestinationSuccessEvent(it))
                }
            } else {
                state.destinationToSet = destination
            }
        }
    }

    private fun removeCurrentDestination(state: PublisherState) {
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
            return distanceFromLastSentLocation >= resolution.minimumDisplacement &&
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

    private inner class PublisherState(
        routingProfile: RoutingProfile,
        var locationEngineResolution: Resolution,
        var isTracking: Boolean = false,
        val trackables: MutableSet<Trackable> = mutableSetOf(),
        val assetStatuses: MutableMap<String, AssetStatus> = mutableMapOf(),
        val assetStatusFlows: MutableMap<String, MutableStateFlow<AssetStatus>> = mutableMapOf(),
        var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, ConnectionState.OFFLINE, null
        ),
        val resolutions: MutableMap<String, Resolution> = mutableMapOf(),
        val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf(),
        var estimatedArrivalTimeInMilliseconds: Long? = null,
        active: Trackable? = null,
        var lastPublisherLocation: Location? = null,
        var destinationToSet: Destination? = null,
        var currentDestination: Destination? = null,
        val subscribers: MutableMap<String, MutableSet<Subscriber>> = mutableMapOf(),
        val requests: MutableMap<String, MutableMap<Subscriber, Resolution>> = mutableMapOf(),
        var presenceData: PresenceData = PresenceData(ClientTypes.PUBLISHER)
    ) {
        var active: Trackable? = active
            set(value) {
                this@DefaultCorePublisher.active = value
                field = value
            }
        var routingProfile: RoutingProfile = routingProfile
            set(value) {
                this@DefaultCorePublisher.routingProfile = value
                field = value
            }
    }
}
