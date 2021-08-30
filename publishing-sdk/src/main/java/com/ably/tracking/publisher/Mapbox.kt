package com.ably.tracking.publisher

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.Location
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.common.ResultHandler
import com.ably.tracking.common.clientOptions
import com.ably.tracking.common.toAssetTracking
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.debug.AblySimulationLocationEngine
import com.ably.tracking.publisher.locationengine.FusedAndroidLocationEngine
import com.ably.tracking.publisher.locationengine.GoogleLocationEngine
import com.ably.tracking.publisher.locationengine.LocationEngineUtils
import com.ably.tracking.publisher.locationengine.ResolutionLocationEngine
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.module.Mapbox_TripNotificationModuleConfiguration
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

typealias LocationHistoryListener = (LocationHistoryData) -> Unit

/**
 * An interface which enables observing location updates.
 */
internal interface LocationUpdatesObserver {
    /**
     * Called when a new raw location update is ready.
     *
     * @param rawLocation the current raw location.
     */
    fun onRawLocationChanged(rawLocation: Location)

    /**
     * Called when a new enhanced location update is ready.
     *
     * @param enhancedLocation the current enhanced location.
     * @param intermediateLocations a list (can be empty) of predicted location points leading up to the current update.
     */
    fun onEnhancedLocationChanged(enhancedLocation: Location, intermediateLocations: List<Location>)
}

/**
 * Wrapper for the [MapboxNavigation] that's used to interact with the Mapbox SDK.
 */
internal interface Mapbox {
    /**
     * Starts the navigation trip which results in location updates from the location engine.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startTrip()

    /**
     * Stops the navigation trip and closes the whole [MapboxNavigation].
     */
    fun stopAndClose()

    /**
     * Sets a location observer that gets notified when a new raw or enhanced location is received.
     * If there is already a registered location observer it will be replaced by the [locationUpdatesObserver].
     *
     * @param locationUpdatesObserver The location observer to register.
     */
    fun registerLocationObserver(locationUpdatesObserver: LocationUpdatesObserver)

    /**
     * Removes a location observer if it was previously set with [registerLocationObserver].
     */
    fun unregisterLocationObserver()

    /**
     * Changes the [resolution] of the location engine if it's a subtype of the [ResolutionLocationEngine].
     *
     * @param resolution The new resolution to set.
     */
    fun changeResolution(resolution: Resolution)

    /**
     * Removes the currently active route.
     */
    fun clearRoute()

    /**
     * Sets a route with the provided parameters. The route starts in [currentLocation] and ends in [destination].
     * When the route is successfully set then it calls the [routeDurationCallback] with the estimated route duration.
     *
     * @param currentLocation The current location of the [Publisher].
     * @param destination The destination of the [Trackable].
     * @param routingProfile The routing profile for the route.
     * @param routeDurationCallback The function that's called with the ETA of the route in milliseconds. If something goes wrong it will be called with [MapException].
     */
    fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: ResultHandler<Long>
    )

    /**
     * Sets a location history listener that will be notified when a trip history is ready.
     *
     * @param listener The function to call when location history data is ready.
     */
    fun setLocationHistoryListener(listener: LocationHistoryListener?)
}

internal class DefaultMapbox(
    context: Context,
    private val mapConfiguration: MapConfiguration,
    connectionConfiguration: ConnectionConfiguration,
    locationSource: LocationSource? = null,
    logHandler: LogHandler?,
    notificationProvider: PublisherNotificationProvider,
    notificationId: Int
) : Mapbox {
    private val mainDispatcher = Dispatchers.Main.immediate
    private var mapboxNavigation: MapboxNavigation
    private var mapboxReplayer: MapboxReplayer? = null
    private var locationHistoryListener: (LocationHistoryListener)? = null
    private var locationObserver: LocationObserver? = null

    init {
        setupTripNotification(notificationProvider, notificationId)
        val mapboxBuilder = MapboxNavigation.defaultNavigationOptionsBuilder(context, mapConfiguration.apiKey)
            .locationEngine(getBestLocationEngine(context, logHandler))
        locationSource?.let {
            when (it) {
                is LocationSourceAbly -> {
                    useAblySimulationLocationEngine(mapboxBuilder, it, connectionConfiguration, logHandler)
                }
                is LocationSourceRaw -> {
                    useHistoryDataReplayerLocationEngine(mapboxBuilder, it)
                }
            }
        }

        runBlocking(mainDispatcher) {
            mapboxNavigation = MapboxNavigation(mapboxBuilder.build())
        }
    }

    private fun setupTripNotification(notificationProvider: PublisherNotificationProvider, notificationId: Int) {
        Mapbox_TripNotificationModuleConfiguration.moduleProvider =
            object : Mapbox_TripNotificationModuleConfiguration.ModuleProvider {
                override fun createTripNotification(): TripNotification =
                    MapboxTripNotification(notificationProvider, notificationId)
            }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startTrip() {
        runBlocking(mainDispatcher) {
            mapboxNavigation.apply {
                toggleHistory(true)
                startTripSession()
            }
        }
    }

    override fun stopAndClose() {
        runBlocking(mainDispatcher) {
            mapboxNavigation.apply {
                stopTripSession()
                mapboxReplayer?.finish()
                val tripHistoryString = retrieveHistory()
                val historyEvents = ReplayHistoryMapper().mapToReplayEvents(tripHistoryString)
                locationHistoryListener?.invoke(LocationHistoryData(historyEvents.toGeoJsonMessages()))
                onDestroy()
            }
        }
    }

    private fun createLocationObserver(locationUpdatesObserver: LocationUpdatesObserver) =
        object : LocationObserver {
            override fun onRawLocationChanged(rawLocation: android.location.Location) {
                locationUpdatesObserver.onRawLocationChanged(rawLocation.toAssetTracking())
            }

            override fun onEnhancedLocationChanged(
                enhancedLocation: android.location.Location,
                keyPoints: List<android.location.Location>
            ) {
                val intermediateLocations =
                    if (keyPoints.size > 1) keyPoints.subList(0, keyPoints.size - 1)
                    else emptyList()
                val currentTimeInMilliseconds = System.currentTimeMillis()
                locationUpdatesObserver.onEnhancedLocationChanged(
                    // Enhanced locations don't have real world timestamps so we use the current device time
                    enhancedLocation.toAssetTracking(currentTimeInMilliseconds),
                    // Intermediate locations should have timestamps in relation to the enhanced location time
                    intermediateLocations.map { location ->
                        val timeDifference = enhancedLocation.time - location.time
                        location.toAssetTracking(currentTimeInMilliseconds - timeDifference)
                    }
                )
            }
        }

    override fun registerLocationObserver(locationUpdatesObserver: LocationUpdatesObserver) {
        unregisterLocationObserver()
        runBlocking(mainDispatcher) {
            locationObserver = createLocationObserver(locationUpdatesObserver)
            locationObserver?.let { mapboxNavigation.registerLocationObserver(it) }
        }
    }

    override fun unregisterLocationObserver() {
        locationObserver?.let {
            runBlocking(mainDispatcher) {
                mapboxNavigation.unregisterLocationObserver(it)
            }
        }
    }

    override fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: ResultHandler<Long>
    ) {
        runBlocking(mainDispatcher) {
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
                            routeDurationCallback(Result.success(routeDurationInMilliseconds.toLong()))
                        }
                    }

                    override fun onRoutesRequestCanceled(routeOptions: RouteOptions) = Unit

                    override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                        // We won't know the ETA for the active trackable and therefore we won't be able to check the temporal threshold.
                        routeDurationCallback(Result.failure(MapException(throwable)))
                    }
                }
            )
        }
    }

    override fun clearRoute() {
        runBlocking(mainDispatcher) {
            mapboxNavigation.setRoutes(emptyList())
        }
    }

    override fun changeResolution(resolution: Resolution) {
        runBlocking(mainDispatcher) {
            mapboxNavigation.navigationOptions.locationEngine.let {
                if (it is ResolutionLocationEngine) {
                    it.changeResolution(resolution)
                }
            }
        }
    }

    private fun getBestLocationEngine(context: Context, logHandler: LogHandler?): ResolutionLocationEngine =
        if (LocationEngineUtils.hasGoogleLocationServices(context)) {
            GoogleLocationEngine(context)
        } else {
            FusedAndroidLocationEngine(context, logHandler)
        }

    private fun useAblySimulationLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceAbly,
        connectionConfiguration: ConnectionConfiguration,
        logHandler: LogHandler?
    ) {
        mapboxBuilder.locationEngine(
            AblySimulationLocationEngine(
                connectionConfiguration.authentication.clientOptions,
                locationSource.simulationChannelName,
                logHandler
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
            val historyEvents = locationSource.historyData.events.toReplayEvents()
            val lastHistoryEvent = historyEvents.last()
            this.pushEvents(historyEvents)
            this.play()
            this.registerObserver(object : ReplayEventsObserver {
                override fun replayEvents(events: List<ReplayEventBase>) {
                    if (events.last() == lastHistoryEvent) {
                        locationSource.onDataEnded?.invoke()
                    }
                }
            })
        }
    }

    override fun setLocationHistoryListener(listener: LocationHistoryListener?) {
        this.locationHistoryListener = listener
    }
}
