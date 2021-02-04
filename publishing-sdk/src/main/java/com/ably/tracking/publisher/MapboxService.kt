package com.ably.tracking.publisher

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
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
import io.ably.lib.types.ClientOptions
import timber.log.Timber

/**
 * Wrapper for the [MapboxNavigation] that's used to interact with the Mapbox SDK.
 */
internal interface MapboxService {
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
     * Adds a location observer that gets notified when a new raw or enhanced location is received.
     *
     * @param locationObserver The location observer to register.
     */
    fun registerLocationObserver(locationObserver: LocationObserver)

    /**
     * Removes a location observer if it was previously added with [registerLocationObserver].
     *
     * @param locationObserver The location observer to remove.
     */
    fun unregisterLocationObserver(locationObserver: LocationObserver)

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
     * @param routeDurationCallback The function that's called with the ETA of the route in milliseconds.
     */
    fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: (durationInMilliseconds: Long) -> Unit
    )
}

internal class DefaultMapboxService(
    context: Context,
    private val mapConfiguration: MapConfiguration,
    connectionConfiguration: ConnectionConfiguration,
    private val debugConfiguration: DebugConfiguration? = null
) : MapboxService {
    private val mapboxNavigation: MapboxNavigation
    private var mapboxReplayer: MapboxReplayer? = null

    init {
        val mapboxBuilder = MapboxNavigation.defaultNavigationOptionsBuilder(
            context,
            mapConfiguration.apiKey
        )
        mapboxBuilder.locationEngine(getBestLocationEngine(context))
        debugConfiguration?.locationSource?.let { locationSource ->
            when (locationSource) {
                is LocationSourceAbly -> {
                    useAblySimulationLocationEngine(mapboxBuilder, locationSource, connectionConfiguration)
                }
                is LocationSourceRaw -> {
                    useHistoryDataReplayerLocationEngine(mapboxBuilder, locationSource)
                }
            }
        }

        mapboxNavigation = MapboxNavigation(mapboxBuilder.build())
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startTrip() {
        mapboxNavigation.apply {
            toggleHistory(true)
            startTripSession()
        }
    }

    override fun stopAndClose() {
        mapboxNavigation.apply {
            stopTripSession()
            mapboxReplayer?.finish()
            debugConfiguration?.locationHistoryHandler?.invoke(retrieveHistory())
            onDestroy()
        }
    }

    override fun registerLocationObserver(locationObserver: LocationObserver) {
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun unregisterLocationObserver(locationObserver: LocationObserver) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    override fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: (durationInMilliseconds: Long) -> Unit
    ) {
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
                        routeDurationCallback(routeDurationInMilliseconds.toLong())
                    }
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) = Unit

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    // We won't know the ETA for the active trackable and therefore we won't be able to check the temporal threshold.
                    Timber.e(throwable, "Failed call to requestRoutes.")
                }
            }
        )
    }

    override fun clearRoute() {
        mapboxNavigation.setRoutes(emptyList())
    }

    override fun changeResolution(resolution: Resolution) {
        mapboxNavigation.navigationOptions.locationEngine.let {
            if (it is ResolutionLocationEngine) {
                it.changeResolution(resolution)
            }
        }
    }

    private fun getBestLocationEngine(context: Context): ResolutionLocationEngine =
        if (LocationEngineUtils.hasGoogleLocationServices(context)) {
            GoogleLocationEngine(context)
        } else {
            FusedAndroidLocationEngine(context)
        }

    private fun useAblySimulationLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceAbly,
        connectionConfiguration: ConnectionConfiguration
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
                        locationSource.onDataEnded?.invoke()
                    }
                }
            })
        }
    }
}
