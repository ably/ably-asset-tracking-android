package com.ably.tracking.locationprovider.mapbox

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.Location
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.common.clientOptions
import com.ably.tracking.common.toAssetTracking
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.locationprovider.Destination
import com.ably.tracking.locationprovider.LocationHistoryListener
import com.ably.tracking.locationprovider.LocationProvider
import com.ably.tracking.locationprovider.LocationUpdatesObserver
import com.ably.tracking.locationprovider.MapException
import com.ably.tracking.locationprovider.RoutingProfile
import com.ably.tracking.locationprovider.mapbox.debug.AblySimulationLocationEngine
import com.ably.tracking.locationprovider.mapbox.locationengine.FusedAndroidLocationEngine
import com.ably.tracking.locationprovider.mapbox.locationengine.GoogleLocationEngine
import com.ably.tracking.locationprovider.mapbox.locationengine.LocationEngineUtils
import com.ably.tracking.locationprovider.mapbox.locationengine.ResolutionLocationEngine
import com.ably.tracking.logging.LogHandler
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.module.Mapbox_TripNotificationModuleConfiguration
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Singleton object used to count the created instances of [MapboxLocationProvider]. This is used to check
 * whether we should destroy the [MapboxNavigation] when a [MapboxLocationProvider] instance is stopped.
 * This object is safe to use across multiple threads as it uses an [AtomicInteger] as the counter.
 */
private object MapboxInstancesCounter {
    private val counter = AtomicInteger(0)
    fun increment() {
        counter.incrementAndGet()
    }

    fun decrementAndCheckIfItWasTheLastOne(): Boolean {
        val instancesAmount = counter.decrementAndGet()
        return instancesAmount == 0
    }
}

class MapboxLocationProvider(
    context: Context,
    private val mapConfiguration: MapConfiguration,
    connectionConfiguration: ConnectionConfiguration,
    locationSource: LocationSource? = null,
    logHandler: LogHandler?,
    notificationProvider: PublisherNotificationProvider,
    notificationId: Int
) : LocationProvider {
    /**
     * Dispatcher used to run [mapboxNavigation] methods on the main thread.
     * It has to be the "immediate" version of the main dispatcher to not
     * block the code execution when it's already called from the main thread.
     */
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
            MapboxInstancesCounter.increment()
            mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
                MapboxNavigationProvider.retrieve()
            } else {
                MapboxNavigationProvider.create(mapboxBuilder.build())
            }
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
            mapboxReplayer?.play()
        }
    }

    override fun stopAndClose() {
        runBlocking(mainDispatcher) {
            mapboxReplayer?.stop()
            mapboxNavigation.apply {
                stopTripSession()
                mapboxReplayer?.finish()
                val tripHistoryString = retrieveHistory()
                // MapBox's mapToReplayEvents method crashes if passed an empty string,
                // so check it's not empty to prevent that.
                // (see: https://github.com/ably/ably-asset-tracking-android/issues/434)
                if (tripHistoryString.isNotEmpty()) {
                    val historyEvents = ReplayHistoryMapper().mapToReplayEvents(tripHistoryString)
                    locationHistoryListener?.invoke(MapboxLocationHistoryData(historyEvents.toLocations()))
                }
            }
            if (MapboxInstancesCounter.decrementAndCheckIfItWasTheLastOne()) {
                MapboxNavigationProvider.destroy()
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
        routeDurationCallback: (Result<Long>) -> Unit
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
