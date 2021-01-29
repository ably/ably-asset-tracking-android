package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.ErrorInformation
import com.ably.tracking.Handler
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.debug.AblySimulationLocationEngine
import com.ably.tracking.publisher.locationengine.FusedAndroidLocationEngine
import com.ably.tracking.publisher.locationengine.GoogleLocationEngine
import com.ably.tracking.publisher.locationengine.LocationEngineUtils
import com.ably.tracking.publisher.locationengine.ResolutionLocationEngine
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.Channel
import io.ably.lib.types.ClientOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val ablyService: AblyService,
    private val connectionConfiguration: ConnectionConfiguration,
    private val mapConfiguration: MapConfiguration,
    private val debugConfiguration: DebugConfiguration?,
    context: Context,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    private var _routingProfile: RoutingProfile
) :
    Publisher {
    private val mapboxNavigation: MapboxNavigation
    private val thresholdChecker = ThresholdChecker()
    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            sendRawLocationMessage(rawLocation)
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            sendEnhancedLocationMessage(enhancedLocation, keyPoints)
        }
    }
    private val presenceData = PresenceData(ClientTypes.PUBLISHER)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventsChannel: SendChannel<Event>
    private val policy: ResolutionPolicy
    private val hooks = Hooks()
    private val methods = Methods()
    private val trackables = mutableSetOf<Trackable>()
    private val requests = mutableMapOf<String, MutableMap<Subscriber, Resolution>>()
    private val subscribers = mutableMapOf<String, MutableSet<Subscriber>>()
    private val resolutions = mutableMapOf<String, Resolution>()
    private var locationEngineResolution: Resolution
    private var isTracking: Boolean = false
    private var mapboxReplayer: MapboxReplayer? = null
    private var lastPublisherLocation: Location? = null
    private var lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
    private var estimatedArrivalTimeInMilliseconds: Long? = null

    /**
     * This field will be set only when trying to set a tracking destination before receiving any [lastPublisherLocation].
     * After successfully setting the tracking destination this field will be set to NULL.
     **/
    private var destinationToSet: Destination? = null
    private var currentDestination: Destination? = null

    private val _locations = MutableSharedFlow<LocationUpdate>()
    private val _connectionStates = MutableSharedFlow<ConnectionStateChange>()
    override val locations: SharedFlow<LocationUpdate>
        get() = _locations.asSharedFlow()
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = _connectionStates.asSharedFlow()

    init {
        eventsChannel = createEventsChannel(scope)
        policy = resolutionPolicyFactory.createResolutionPolicy(
            hooks,
            methods
        )
        locationEngineResolution = policy.resolve(emptySet())

        Timber.w("Started.")

        val mapboxBuilder = MapboxNavigation.defaultNavigationOptionsBuilder(
            context,
            mapConfiguration.apiKey
        )
        mapboxBuilder.locationEngine(getBestLocationEngine(context))
        debugConfiguration?.locationSource?.let { locationSource ->
            when (locationSource) {
                is LocationSourceAbly -> {
                    useAblySimulationLocationEngine(mapboxBuilder, locationSource)
                }
                is LocationSourceRaw -> {
                    useHistoryDataReplayerLocationEngine(mapboxBuilder, locationSource)
                }
            }
        }

        ablyService.subscribeForAblyStateChange { state -> scope.launch { _connectionStates.emit(state) } }

        mapboxNavigation = MapboxNavigation(mapboxBuilder.build())
        mapboxNavigation.registerLocationObserver(locationObserver)
        startLocationUpdates()
    }

    private fun getBestLocationEngine(context: Context): ResolutionLocationEngine =
        if (LocationEngineUtils.hasGoogleLocationServices(context)) {
            GoogleLocationEngine(context)
        } else {
            FusedAndroidLocationEngine(context)
        }

    private fun useAblySimulationLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceAbly
    ) {
        mapboxBuilder.locationEngine(
            AblySimulationLocationEngine(
                // TODO should there be a clientId in use here?
                ClientOptions(connectionConfiguration.apiKey),
                locationSource.simulationChannelName
            )
        )
    }

    private fun useHistoryDataReplayerLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceRaw
    ) {
        mapboxReplayer = MapboxReplayer().apply {
            mapboxBuilder.locationEngine(ReplayLocationEngine(this))
            this.clearEvents()
            val historyEvents = ReplayHistoryMapper().mapToReplayEvents(locationSource.historyData)
            val lastHistoryEvent = historyEvents.last()
            this.pushEvents(historyEvents)
            this.play()
            this.registerObserver(object : ReplayEventsObserver {
                override fun replayEvents(events: List<ReplayEventBase>) {
                    if (events.last() == lastHistoryEvent) {
                        locationSource.onDataEnded?.let { handler -> callback(handler, Unit) }
                    }
                }
            })
        }
    }

    private fun sendRawLocationMessage(rawLocation: Location) {
        enqueue(RawLocationChangedEvent(LocationUpdate(rawLocation)))
    }

    private fun performRawLocationChanged(event: RawLocationChangedEvent) {
        lastPublisherLocation = event.locationUpdate.location
        destinationToSet?.let { setDestination(it) }
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val intermediateLocations = if (keyPoints.size > 1) keyPoints.subList(0, keyPoints.size - 1) else emptyList()
        enqueue(
            EnhancedLocationChangedEvent(
                EnhancedLocationUpdate(
                    enhancedLocation,
                    intermediateLocations,
                    if (intermediateLocations.isEmpty()) LocationUpdateType.ACTUAL else LocationUpdateType.PREDICTED
                )
            )
        )
    }

    private fun performEnhancedLocationChanged(event: EnhancedLocationChangedEvent) {
        for (trackable in trackables) {
            if (shouldSendLocation(event.locationUpdate.location, lastSentEnhancedLocations[trackable.id], trackable)) {
                lastSentEnhancedLocations[trackable.id] = event.locationUpdate.location
                ablyService.sendEnhancedLocation(trackable.id, event.locationUpdate)
            }
        }
        scope.launch { _locations.emit(event.locationUpdate) }
        checkThreshold(event.locationUpdate.location)
    }

    private fun shouldSendLocation(
        currentLocation: Location,
        lastSentLocation: Location?,
        trackable: Trackable
    ): Boolean {
        val resolution = resolutions[trackable.id]
        return if (resolution != null && lastSentLocation != null) {
            val timeSinceLastSentLocation = currentLocation.timeFrom(lastSentLocation)
            val distanceFromLastSentLocation = currentLocation.distanceInMetersFrom(lastSentLocation)
            return distanceFromLastSentLocation >= resolution.minimumDisplacement &&
                timeSinceLastSentLocation >= resolution.desiredInterval
        } else {
            true
        }
    }

    private fun checkThreshold(currentLocation: Location) {
        methods.threshold?.let { threshold ->
            if (thresholdChecker.isThresholdReached(
                    threshold,
                    currentLocation,
                    System.currentTimeMillis(),
                    active?.destination,
                    estimatedArrivalTimeInMilliseconds
                )
            ) {
                methods.onProximityReached()
            }
        }
    }

    private fun startLocationUpdates() {
        enqueue(StartEvent())
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun performStartPublisher() {
        if (!isTracking) {
            isTracking = true

            Timber.e("startLocationUpdates")

            mapboxNavigation.apply {
                toggleHistory(true)
                startTripSession()
            }
        }
    }

//    override fun track(trackable: Trackable, handler: ResultHandler<Unit>) {
//        enqueue(TrackTrackableEvent(trackable, handler))
//    }

//    override fun track(trackable: Trackable, listener: ResultListener<Void?>) {
//        track(trackable) { listener.onResult(it.toJava()) }
//    }

    private fun performTrackTrackable(event: TrackTrackableEvent) {
        createChannelForTrackableIfNotExisits(event.trackable) {
            if (it.isSuccess) {
                enqueue(SetActiveTrackableEvent(event.trackable, event.handler))
            } else {
                callback(event.handler, it)
            }
        }
    }

    private fun performSetActiveTrackableEvent(event: SetActiveTrackableEvent) {
        if (active != event.trackable) {
            active = event.trackable
            hooks.trackables?.onActiveTrackableChanged(event.trackable)
            event.trackable.destination?.let {
                setDestination(it)
            }
        }
        callback(event.handler, Result.success(Unit))
    }

//    override fun add(trackable: Trackable, handler: ResultHandler<Unit>) {
//        enqueue(AddTrackableEvent(trackable, handler))
//    }

//    override fun add(trackable: Trackable, listener: ResultListener<Void?>) {
//        add(trackable) { listener.onResult(it.toJava()) }
//    }

    private fun performAddTrackable(event: AddTrackableEvent) {
        createChannelForTrackableIfNotExisits(event.trackable, event.handler)
    }

    /**
     * Creates a [Channel] for the [Trackable], joins the channel's presence and enqueues [SuccessEvent].
     * If a [Channel] for the given [Trackable] exists then it just enqueues [SuccessEvent].
     * If during channel creation and joining presence an error occurs then it enqueues [FailureEvent] with the exception.
     */
    private fun createChannelForTrackableIfNotExisits(
        trackable: Trackable,
        handler: ResultHandler<Unit>
    ) {
        ablyService.connect(trackable.id, presenceData) { result ->
            if (result.isSuccess) {
                ablyService.subscribeForPresenceMessages(trackable.id) { enqueue(PresenceMessageEvent(trackable, it)) }
                enqueue(JoinPresenceSuccessEvent(trackable, handler))
            } else {
                // TODO - is this correct in case of an error?
                callback(handler, result)
            }
        }
    }

    private fun performJoinPresenceSuccess(event: JoinPresenceSuccessEvent) {
        trackables.add(event.trackable)
        resolveResolution(event.trackable)
        hooks.trackables?.onTrackableAdded(event.trackable)
        callback(event.handler, Result.success(Unit))
    }

//    override fun remove(trackable: Trackable, handler: ResultHandler<Boolean>) {
//        enqueue(RemoveTrackableEvent(trackable, handler))
//    }

//    override fun remove(trackable: Trackable, listener: ResultListener<Boolean>) {
//        remove(trackable) { listener.onResult(it) }
//    }

    private fun performRemoveTrackable(event: RemoveTrackableEvent) {
        val wasTrackablePresent = trackables.remove(event.trackable)
        if (wasTrackablePresent) {
            hooks.trackables?.onTrackableRemoved(event.trackable)
            removeAllSubscribers(event.trackable)
            resolutions.remove(event.trackable.id)?.let { enqueue(ChangeLocationEngineResolutionEvent()) }
            requests.remove(event.trackable.id)
            lastSentEnhancedLocations.remove(event.trackable.id)

            // If this was the active Trackable then clear that state and remove destination.
            if (active == event.trackable) {
                removeCurrentDestination()
                active = null
                hooks.trackables?.onActiveTrackableChanged(null)
            }

            // Leave Ably channel.
            ablyService.disconnect(event.trackable.id, presenceData){
                if(it.isSuccess){
                    callback(event.handler, Result.success(true))
                }else {
                    // TODO - callback handler with error
//                    callback(event.handler, it.exceptionOrNull()!!)
                }
            }
        } else {
            // notify with false to indicate that it was not removed
            callback(event.handler, Result.success(false))
        }
    }

    private fun removeAllSubscribers(trackable: Trackable) {
        subscribers[trackable.id]?.let { subscribers ->
            subscribers.forEach { hooks.subscribers?.onSubscriberRemoved(it) }
            subscribers.clear()
        }
    }

    override var active: Trackable? = null

    override var routingProfile: RoutingProfile
        get() = _routingProfile
        set(value) {
            enqueue(ChangeRoutingProfileEvent(value))
        }

//    override fun stop(handler: ResultHandler<Unit>) {
//        enqueue(StopEvent(handler))
//    }

//    override fun stop(listener: ResultListener<Void?>) {
//        stop() { listener.onResult(it.toJava()) }
//    }

    private fun performPresenceMessage(event: PresenceMessageEvent) {
        when (event.presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    addSubscriber(event.presenceMessage.clientId, event.trackable, event.presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    removeSubscriber(event.presenceMessage.clientId, event.trackable)
                }
            }
            PresenceAction.UPDATE -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    updateSubscriber(event.presenceMessage.clientId, event.trackable, event.presenceMessage.data)
                }
            }
        }
    }

    private fun addSubscriber(id: String, trackable: Trackable, data: PresenceData) {
        val subscriber = Subscriber(id, trackable)
        if (subscribers[trackable.id] == null) {
            subscribers[trackable.id] = mutableSetOf()
        }
        subscribers[trackable.id]?.add(subscriber)
        saveOrRemoveResolutionRequest(data.resolution, trackable, subscriber)
        hooks.subscribers?.onSubscriberAdded(subscriber)
        resolveResolution(trackable)
    }

    private fun updateSubscriber(id: String, trackable: Trackable, data: PresenceData) {
        subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                data.resolution.let { resolution ->
                    saveOrRemoveResolutionRequest(resolution, trackable, subscriber)
                    resolveResolution(trackable)
                }
            }
        }
    }

    private fun removeSubscriber(id: String, trackable: Trackable) {
        subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                subscribers.remove(subscriber)
                requests[trackable.id]?.remove(subscriber)
                hooks.subscribers?.onSubscriberRemoved(subscriber)
                resolveResolution(trackable)
            }
        }
    }

    private fun saveOrRemoveResolutionRequest(resolution: Resolution?, trackable: Trackable, subscriber: Subscriber) {
        if (resolution != null) {
            if (requests[trackable.id] == null) {
                requests[trackable.id] = mutableMapOf()
            }
            requests[trackable.id]?.put(subscriber, resolution)
        } else {
            requests[trackable.id]?.remove(subscriber)
        }
    }

    private fun performChangeRoutingProfile(event: ChangeRoutingProfileEvent) {
        _routingProfile = event.routingProfile
        currentDestination?.let { setDestination(it) }
    }

    private fun performStopPublisher(event: StopEvent) {
        ablyService.close(presenceData)
        stopLocationUpdates()

        // TODO implement proper stopping strategy which only calls back once we're fully stopped (considering whether scope.cancel() is appropriate)
        callback(event.handler, Result.success(Unit))
    }

    private fun stopLocationUpdates() {
        if (isTracking) {
            isTracking = false
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.apply {
                stopTripSession()
                mapboxReplayer?.finish()
                // TODO - add some kind of a listener for history data to the API
//                debugConfiguration?.locationHistoryHandler?.invoke(retrieveHistory())
                onDestroy()
            }
        }
    }

    /**
     * This method must be called from the publishers event queue.
     */
    private fun setDestination(destination: Destination) {
        // TODO is there a way to ensure we're executing in the right thread?
        lastPublisherLocation.let { currentLocation ->
            if (currentLocation != null) {
                destinationToSet = null
                removeCurrentDestination()
                currentDestination = destination
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultParams()
                        .accessToken(mapConfiguration.apiKey)
                        .coordinates(getRouteCoordinates(currentLocation, destination))
                        .profile(routingProfile.toMapboxProfileName())
                        .build(),
                    object : RoutesRequestCallback {
                        override fun onRoutesReady(routes: List<DirectionsRoute>) {
                            routes.firstOrNull()?.let {
                                val routeDurationInMilliseconds =
                                    (it.durationTypical() ?: it.duration()) * MILLISECONDS_PER_SECOND
                                enqueue(SetDestinationSuccessEvent(routeDurationInMilliseconds.toLong()))
                            }
                        }

                        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) = Unit

                        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                            // We won't know the ETA for the active trackable and therefore we won't be able to check the temporal threshold.
                            Timber.e(throwable, "Failed call to requestRoutes.")
                        }
                    }
                )
            } else {
                destinationToSet = destination
            }
        }
    }

    private fun performSetDestinationSuccess(event: SetDestinationSuccessEvent) {
        estimatedArrivalTimeInMilliseconds = System.currentTimeMillis() + event.routeDurationInMilliseconds
    }

    private fun removeCurrentDestination() {
        mapboxNavigation.setRoutes(emptyList())
        currentDestination = null
        estimatedArrivalTimeInMilliseconds = null
    }

    private fun performRefreshResolutionPolicy() {
        trackables.forEach { resolveResolution(it) }
    }

    private fun resolveResolution(trackable: Trackable) {
        val resolutionRequests: Set<Resolution> = requests[trackable.id]?.values?.toSet() ?: emptySet()
        policy.resolve(TrackableResolutionRequest(trackable, resolutionRequests)).let { resolution ->
            resolutions[trackable.id] = resolution
            enqueue(ChangeLocationEngineResolutionEvent())
        }
    }

    private fun performChangeLocationEngineResolution() {
        locationEngineResolution = policy.resolve(resolutions.values.toSet())
        changeLocationEngineResolution(locationEngineResolution)
    }

    private fun changeLocationEngineResolution(resolution: Resolution) {
        mapboxNavigation.navigationOptions.locationEngine.let {
            if (it is ResolutionLocationEngine) {
                it.changeResolution(resolution)
            }
        }
    }

    private fun postToMainThread(operation: () -> Unit) {
        android.os.Handler(getLooperForMainThread()).post(operation)
    }

    private fun getLooperForMainThread() = android.os.Looper.getMainLooper()

    @OptIn(ObsoleteCoroutinesApi::class)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<Event> {
            for (event in channel) {
                when (event) {
                    is AddTrackableEvent -> performAddTrackable(event)
                    is TrackTrackableEvent -> performTrackTrackable(event)
                    is RemoveTrackableEvent -> performRemoveTrackable(event)
                    is StopEvent -> performStopPublisher(event)
                    is StartEvent -> performStartPublisher()
                    is JoinPresenceSuccessEvent -> performJoinPresenceSuccess(event)
                    is RawLocationChangedEvent -> performRawLocationChanged(event)
                    is EnhancedLocationChangedEvent -> performEnhancedLocationChanged(event)
                    is RefreshResolutionPolicyEvent -> performRefreshResolutionPolicy()
                    is SetDestinationSuccessEvent -> performSetDestinationSuccess(event)
                    is PresenceMessageEvent -> performPresenceMessage(event)
                    is ChangeLocationEngineResolutionEvent -> performChangeLocationEngineResolution()
                    is SetActiveTrackableEvent -> performSetActiveTrackableEvent(event)
                    is ChangeRoutingProfileEvent -> performChangeRoutingProfile(event)
                }
            }
        }

    /**
     * Send a failure event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: ResultHandler<T>, errorInformation: ErrorInformation) {
        // TODO - shouldn't we somehow keep all ErrorInformation data in the exception?
        callback(handler, Result.failure(Exception(errorInformation.message)))
    }

    /**
     * Send an event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: Handler<T>, result: T) {
        scope.launch(Dispatchers.Main) { handler(result) }
    }

    private fun enqueue(event: Event) {
        scope.launch { eventsChannel.send(event) }
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

    override suspend fun track(trackable: Trackable) {
        TODO("Not yet implemented")
    }

    override suspend fun add(trackable: Trackable) {
        TODO("Not yet implemented")
    }

    override suspend fun remove(trackable: Trackable): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }
}
