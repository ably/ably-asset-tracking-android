package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.toGeoJson
import com.ably.tracking.common.toJsonArray
import com.ably.tracking.publisher.debug.AblySimulationLocationEngine
import com.google.gson.Gson
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val connectionConfiguration: ConnectionConfiguration,
    private val mapConfiguration: MapConfiguration,
    private val debugConfiguration: DebugConfiguration?,
    private val locationUpdatedListener: LocationUpdatedListener,
    context: Context,
    resolutionPolicyFactory: ResolutionPolicy.Factory
) :
    Publisher {
    private val gson: Gson = Gson()
    private val mapboxNavigation: MapboxNavigation
    private val ably: AblyRealtime
    private val channelMap: MutableMap<String, Channel> = mutableMapOf()
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
    private val eventsChannel: SendChannel<PublisherEvent>
    private val resolutionPolicy: ResolutionPolicy
    private val resolutionPolicyHooks = DefaultHooks()
    private val resolutionPolicyMethods = DefaultMethods()
    private var isTracking: Boolean = false
    private var mapboxReplayer: MapboxReplayer? = null
    private var lastKnownLocation: Location? = null

    /**
     * This field will be set only when trying to set a tracking destination before receiving any [lastKnownLocation].
     * After successfully setting the tracking destination this field will be set to NULL.
     **/
    private var destinationToSet: Destination? = null

    init {
        eventsChannel = createEventsChannel(scope)
        resolutionPolicy = resolutionPolicyFactory.createResolutionPolicy(
            resolutionPolicyHooks,
            resolutionPolicyMethods
        )
        ably = AblyRealtime(connectionConfiguration.apiKey)

        Timber.w("Started.")

        val mapboxBuilder = MapboxNavigation.defaultNavigationOptionsBuilder(
            context,
            mapConfiguration.apiKey
        )
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

        debugConfiguration?.ablyStateChangeListener?.let { ablyStateChangeListener ->
            ably.connection.on { state -> postToMainThread { ablyStateChangeListener(state) } }
        }

        mapboxNavigation = MapboxNavigation(mapboxBuilder.build())
        mapboxNavigation.registerLocationObserver(locationObserver)
        startLocationUpdates()
    }

    private fun useAblySimulationLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceAbly
    ) {
        mapboxBuilder.locationEngine(
            AblySimulationLocationEngine(
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
            this.pushEvents(ReplayHistoryMapper().mapToReplayEvents(locationSource.historyData))
            this.play()
        }
    }

    private fun sendRawLocationMessage(rawLocation: Location) {
        enqueue(RawLocationChangedEvent(rawLocation, rawLocation.toGeoJson()))
    }

    private fun performRawLocationChanged(event: RawLocationChangedEvent) {
        lastKnownLocation = event.location
        destinationToSet?.let { setDestination(it) }
        Timber.d("sendRawLocationMessage: publishing: ${event.geoJsonMessage.synopsis()}")
        channelMap.values.forEach {
            it.publish(EventNames.RAW, event.geoJsonMessage.toJsonArray(gson))
        }
        enqueue(SuccessEvent { locationUpdatedListener(event.location) })
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        enqueue(EnhancedLocationChangedEvent(enhancedLocation, geoJsonMessages))
    }

    private fun performEnhancedLocationChanged(event: EnhancedLocationChangedEvent) {
        event.geoJsonMessages.forEach {
            Timber.d("sendEnhancedLocationMessage: publishing: ${it.synopsis()}")
        }
        channelMap.values.forEach {
            it.publish(EventNames.ENHANCED, event.geoJsonMessages.toJsonArray(gson))
        }
        enqueue(SuccessEvent { locationUpdatedListener(event.location) })
    }

    private fun startLocationUpdates() {
        enqueue(StartPublisherEvent())
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

    override fun track(trackable: Trackable, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        enqueue(TrackTrackableEvent(trackable, onSuccess, onError))
    }

    private fun performTrackTrackable(event: TrackTrackableEvent) {
        if (this.active != null) {
            enqueue(
                ErrorEvent(
                    IllegalStateException("For this preview version of the SDK, this method may only be called once for any given instance of this class."),
                    event.onError
                )
            )
        }

        createChannelForTrackableIfNotExisits(
            event.trackable,
            { enqueue(TrackableReadyToTrackEvent(event.trackable, event.onSuccess)) },
            event.onError
        )
    }

    private fun performTrackableReadyToTrack(event: TrackableReadyToTrackEvent) {
        if (active != event.trackable) {
            active = event.trackable
            event.trackable.destination?.let { setDestination(it) }
        }
        enqueue(SuccessEvent(event.onSuccess))
    }

    override fun add(trackable: Trackable, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        enqueue(AddTrackableEvent(trackable, onSuccess, onError))
    }

    private fun performAddTrackable(event: AddTrackableEvent) {
        createChannelForTrackableIfNotExisits(event.trackable, event.onSuccess, event.onError)
    }

    /**
     * Creates a [Channel] for the [Trackable], joins the channel's presence and enqueues [SuccessEvent].
     * If a [Channel] for the given [Trackable] exists then it just enqueues [SuccessEvent].
     * If during channel creation and joining presence an error occurs then it enqueues [ErrorEvent] with the exception.
     */
    private fun createChannelForTrackableIfNotExisits(
        trackable: Trackable,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!channelMap.contains(trackable.id)) {
            createChannelAndJoinPresence(trackable, onSuccess, onError)
        } else {
            enqueue(SuccessEvent(onSuccess))
        }
    }

    private fun performJoinPresenceSuccess(event: JoinPresenceSuccessEvent) {
        channelMap[event.trackable.id] = event.channel
        enqueue(SuccessEvent(event.onSuccess))
    }

    override fun remove(
        trackable: Trackable,
        onSuccess: (wasPresent: Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        enqueue(RemoveTrackableEvent(trackable, onSuccess, onError))
    }

    private fun performRemoveTrackable(event: RemoveTrackableEvent) {
        val removedChannel = channelMap.remove(event.trackable.id)
        if (removedChannel != null) {
            leaveChannelPresence(
                removedChannel,
                { enqueue(ClearActiveTrackableEvent(event.trackable) { event.onSuccess(true) }) },
                event.onError
            )
        } else {
            enqueue(SuccessEvent { event.onSuccess(false) })
        }
    }

    private fun performClearActiveTrackable(event: ClearActiveTrackableEvent) {
        if (active == event.trackable) {
            removeCurrentDestination()
            active = null
        }
        enqueue(SuccessEvent(event.onSuccess))
    }

    override var active: Trackable? = null

    /**
     * Creates a [Channel] for the [Trackable] and joins the channel's presence.
     * If successfully enters presence then it enqueues [JoinPresenceSuccessEvent] with the created [Channel].
     * If an error occurs during that process then it enqueues [ErrorEvent] with the exception.
     */
    private fun createChannelAndJoinPresence(
        trackable: Trackable,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        ably.channels.get(trackable.id).apply {
            try {
                presence.enterClient(
                    connectionConfiguration.clientId,
                    gson.toJson(presenceData),
                    object : CompletionListener {
                        override fun onSuccess() {
                            enqueue(JoinPresenceSuccessEvent(trackable, this@apply, onSuccess))
                        }

                        override fun onError(reason: ErrorInfo?) {
                            val errorMessage = "Unable to enter presence: ${reason?.message}"
                            Timber.e(errorMessage)
                            enqueue(ErrorEvent(Exception(errorMessage), onError))
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                Timber.e(ablyException)
                enqueue(ErrorEvent(Exception(ablyException), onError))
            }
        }
    }

    /**
     * Leaves the given [Channel]'s presence.
     * If successfully leaves presence then it enqueues [SuccessEvent].
     * If an error occurs during that process then it enqueues [ErrorEvent] with the exception.
     */
    private fun leaveChannelPresence(
        channel: Channel,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        leaveChannelPresenceOmittingQueue(
            channel,
            { enqueue(SuccessEvent(onSuccess)) },
            { enqueue(ErrorEvent(it, onError)) }
        )
    }

    /**
     * Leaves the given [Channel]'s presence without enqueueing any events.
     * If successfully leaves presence then it calls [onSuccess].
     * If an error occurs during that process then it calls [onError] with the exception.
     */
    private fun leaveChannelPresenceOmittingQueue(
        channel: Channel,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            channel.presence.leaveClient(
                connectionConfiguration.clientId,
                gson.toJson(presenceData),
                object : CompletionListener {
                    override fun onSuccess() {
                        onSuccess()
                    }

                    override fun onError(reason: ErrorInfo?) {
                        val errorMessage = "Unable to leave presence: ${reason?.message}"
                        Timber.e(errorMessage)
                        onError(Exception(errorMessage))
                    }
                }
            )
        } catch (ablyException: AblyException) {
            Timber.e(ablyException)
            onError(ablyException)
        }
    }

    override var transportationMode: TransportationMode
        get() = TODO("Not yet implemented")
        set(value) {
            TODO("Not yet implemented")
        }

    override fun stop() {
        enqueue(StopPublisherEvent())
    }

    private fun performStopPublisher() {
        stopLocationUpdates()
        ably.close()
        scope.cancel()
    }

    private fun stopLocationUpdates() {
        if (isTracking) {
            isTracking = false
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            channelMap.apply {
                values.forEach {
                    leaveChannelPresenceOmittingQueue(it, {}, { error -> Timber.e(error) })
                }
                clear()
            }
            mapboxReplayer?.finish()
            debugConfiguration?.locationHistoryReadyListener?.invoke(mapboxNavigation.retrieveHistory())
            mapboxNavigation.apply {
                toggleHistory(false)
                toggleHistory(true)
            }
        }
    }

    private fun setDestination(destination: Destination) {
        enqueue(SetDestinationEvent(destination))
    }

    private fun performSetDestination(event: SetDestinationEvent) {
        lastKnownLocation.let { currentLocation ->
            if (currentLocation != null) {
                destinationToSet = null
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultParams()
                        .accessToken(mapConfiguration.apiKey)
                        .coordinates(getRouteCoordinates(currentLocation, event.destination))
                        .build()
                )
            } else {
                destinationToSet = event.destination
            }
        }
    }

    private fun removeCurrentDestination() {
        mapboxNavigation.setRoutes(emptyList())
    }

    private fun performRefreshResolutionPolicy() {
    }

    private fun postToMainThread(operation: () -> Unit) {
        Handler(getLooperForMainThread()).post(operation)
    }

    private fun getLooperForMainThread() = Looper.getMainLooper()

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<PublisherEvent> {
            for (event in channel) {
                when (event) {
                    is AddTrackableEvent -> performAddTrackable(event)
                    is TrackTrackableEvent -> performTrackTrackable(event)
                    is RemoveTrackableEvent -> performRemoveTrackable(event)
                    is StopPublisherEvent -> performStopPublisher()
                    is StartPublisherEvent -> performStartPublisher()
                    is JoinPresenceSuccessEvent -> performJoinPresenceSuccess(event)
                    is TrackableReadyToTrackEvent -> performTrackableReadyToTrack(event)
                    is SuccessEvent -> performEventSuccess(event)
                    is ErrorEvent -> performEventError(event)
                    is ClearActiveTrackableEvent -> performClearActiveTrackable(event)
                    is RawLocationChangedEvent -> performRawLocationChanged(event)
                    is EnhancedLocationChangedEvent -> performEnhancedLocationChanged(event)
                    is SetDestinationEvent -> performSetDestination(event)
                    is RefreshResolutionPolicyEvent -> performRefreshResolutionPolicy()
                }
            }
        }

    private fun performEventSuccess(event: SuccessEvent) {
        callback { event.onSuccess() }
    }

    private fun performEventError(event: ErrorEvent) {
        callback { event.onError(event.exception) }
    }

    private fun callback(action: () -> Unit) {
        scope.launch(Dispatchers.Main) { action() }
    }

    private fun enqueue(event: PublisherEvent) {
        scope.launch { eventsChannel.send(event) }
    }

    private inner class DefaultHooks : ResolutionPolicy.Hooks {
        var trackables: ResolutionPolicy.Hooks.TrackableSetListener? = null
        var subscribers: ResolutionPolicy.Hooks.SubscriberSetListener? = null

        override fun trackables(listener: ResolutionPolicy.Hooks.TrackableSetListener) {
            trackables = listener
        }

        override fun subscribers(listener: ResolutionPolicy.Hooks.SubscriberSetListener) {
            subscribers = listener
        }
    }

    private inner class DefaultMethods : ResolutionPolicy.Methods {
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
}
