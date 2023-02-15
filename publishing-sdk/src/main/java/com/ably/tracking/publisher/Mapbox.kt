package com.ably.tracking.publisher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.ably.tracking.Location
import com.ably.tracking.LocationValidationException
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.clientOptions
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.e
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.toAssetTracking
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.debug.AblySimulationLocationEngine
import com.ably.tracking.publisher.debug.FlowLocationEngine
import com.ably.tracking.publisher.locationengine.FusedAndroidLocationEngine
import com.ably.tracking.publisher.locationengine.GoogleLocationEngine
import com.ably.tracking.publisher.locationengine.LocationEngineUtils
import com.ably.tracking.publisher.locationengine.ResolutionLocationEngine
import com.ably.tracking.publisher.locationengine.toLocationEngineRequest
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.module.Mapbox_TripNotificationModuleConfiguration
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.internal.utils.InternalUtils
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import java.util.concurrent.atomic.AtomicInteger
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
     * Stops the navigation trip.
     */
    fun stopTrip()

    /**
     * Closes the whole [MapboxNavigation].
     */
    fun close()

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
        routeDurationCallback: ResultCallbackFunction<Long>
    )

    /**
     * Sets a location history listener that will be notified when a trip history is ready.
     *
     * @param listener The function to call when location history data is ready.
     */
    fun setLocationHistoryListener(listener: LocationHistoryListener?)
}

/**
 * Singleton object used to hold the created instances of [Mapbox] interface implementations ([DefaultMapbox]).
 * Uses reference counting to check whether we should destroy the [MapboxNavigation] when a [Mapbox] instance is stopped.
 * This object is safe to use across multiple threads as it uses [synchronized], an [AtomicInteger] as the counter, and [Volatile] annotation on the instance field.
 */
private object MapboxInstanceProvider {

    @Volatile
    private var mapboxNavigation: MapboxNavigation? = null

    private val counter = AtomicInteger(0)

    /**
     * Call to get a MapboxNavigation instance. When no instance is already created will create a new one using provided options.
     *
     * @param navigationOptions options to be used if MapboxNavigation needs to be instantiated.
     */

    @Suppress("VisibleForTests")
    fun createOrRetrieve(navigationOptions: NavigationOptions): MapboxNavigation =
        synchronized(this) {
            counter.incrementAndGet()
            mapboxNavigation ?: MapboxNavigation(navigationOptions).also { mapboxNavigation = it }
        }

    /**
     * Call when previously obtained MapboxNavigation instance is no longer used. Will call onDestroy if this was the last reference.
     *
     * @return A [Boolean] that indicates whether MapboxNavigation was destroyed
     */
    fun destroyIfPossible(): Boolean =
        synchronized(this) {
            val wasLastInstance = counter.decrementAndGet() == 0
            if (wasLastInstance) {
                mapboxNavigation?.onDestroy()
                mapboxNavigation = null
            }
            wasLastInstance
        }
}

/**
 *  A standalone and independently testable utility class used by the DefaultMapbox implementation
 *  to create LocationObserver instances which validate, sanitize and transform mapbox locations
 *  before sending them on to AAT.
 */
internal class MapboxLocationObserverProvider(
    private val logHandler: LogHandler?,
    private val timeProvider: TimeProvider,
    private val TAG: String
) {
    fun createLocationObserver(locationUpdatesObserver: LocationUpdatesObserver) =
        object : LocationObserver {
            override fun onNewRawLocation(rawLocation: android.location.Location) {
                logHandler?.v("$TAG Raw location received from Mapbox: $rawLocation")
                val rawLocationResult = rawLocation.toAssetTracking()
                try {
                    locationUpdatesObserver.onRawLocationChanged(rawLocationResult.getOrThrow())
                } catch (locationValidationException: LocationValidationException) {
                    logHandler?.v("$TAG Swallowing invalid raw location from Mapbox, validation exception was: $locationValidationException")
                }
            }

            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                val enhancedLocation = locationMatcherResult.enhancedLocation
                val keyPoints = locationMatcherResult.keyPoints
                logHandler?.v("$TAG Enhanced location received from Mapbox: $enhancedLocation")
                val intermediateLocations =
                    if (keyPoints.size > 1) keyPoints.subList(0, keyPoints.size - 1)
                    else emptyList()
                val currentTimeInMilliseconds = timeProvider.getCurrentTime()
                // Enhanced locations don't have real world timestamps so we use the current device time
                val enhancedLocationResult =
                    enhancedLocation.toAssetTracking(currentTimeInMilliseconds)
                try {
                    locationUpdatesObserver.onEnhancedLocationChanged(
                        enhancedLocationResult.getOrThrow(),
                        intermediateLocations.mapNotNull { location ->
                            val timeDifference = enhancedLocation.time - location.time
                            // Intermediate locations should have timestamps in relation to the enhanced location time
                            val intermediateLocationResult =
                                location.toAssetTracking(currentTimeInMilliseconds - timeDifference)
                            try {
                                intermediateLocationResult.getOrThrow()
                            } catch (locationValidationException: LocationValidationException) {
                                logHandler?.v("$TAG Swallowing invalid intermediate location from Mapbox, validation exception was: ${intermediateLocationResult.exceptionOrNull()}")
                                null
                            }
                        }
                    )
                } catch (locationValidationException: LocationValidationException) {
                    logHandler?.v("$TAG Swallowing invalid enhanced location from Mapbox, validation exception was: $locationValidationException")
                }
            }
        }
}

/**
 * The special Mapbox configuration that enables the Asset Tracking Profile (a.k.a. the Cycling Profile).
 */
private const val ASSET_TRACKING_PROFILE_ENABLED_CONFIGURATION: String = """
{
   "cache": {
       "enableAssetsTrackingMode": true
   },
   "navigation": {
       "routeLineFallbackPolicy": {
           "policy": 1
       }
   }
}
"""

/**
 * The Mapbox configuration that disables the Asset Tracking Profile (a.k.a. the Cycling Profile).
 */
private const val ASSET_TRACKING_PROFILE_DISABLED_CONFIGURATION: String = ""

/**
 * The default implementation of the [Mapbox] wrapper.
 * The [MapboxNavigation] needs to be called from the main thread. To achieve that we use the [runBlocking] method with the [mainDispatcher].
 * This enables us to switch threads and run the required method in the main thread.
 */
internal class DefaultMapbox(
    private val context: Context,
    mapConfiguration: MapConfiguration,
    connectionConfiguration: ConnectionConfiguration,
    locationSource: LocationSource? = null,
    private val logHandler: LogHandler?,
    private val notificationProvider: PublisherNotificationProvider,
    private val notificationId: Int,
    private val rawHistoryCallback: ((String) -> Unit)?,
    constantLocationEngineResolution: Resolution?,
    vehicleProfile: VehicleProfile,
) : Mapbox {
    private val TAG = createLoggingTag(this)

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
    private lateinit var arrivalObserver: ArrivalObserver
    private val mapboxLocationObserverProvider = MapboxLocationObserverProvider(logHandler, TimeProvider(), TAG)

    init {
        setupTripNotification(notificationProvider, notificationId)
        val mapboxBuilder = NavigationOptions.Builder(context).accessToken(mapConfiguration.apiKey)
            .deviceProfile(createDeviceProfile(vehicleProfile))
            .navigatorPredictionMillis(0L) // Setting this to 0 disables location predictions
            .locationEngine(getBestLocationEngine(context, logHandler))
        locationSource?.let {
            when (it) {
                is LocationSourceAbly -> {
                    logHandler?.v("$TAG Use Ably simulation location engine")
                    useAblySimulationLocationEngine(mapboxBuilder, it, connectionConfiguration, logHandler)
                }
                is LocationSourceRaw -> {
                    logHandler?.v("$TAG Use history data replayer location engine")
                    useHistoryDataReplayerLocationEngine(mapboxBuilder, it)
                }
                is LocationSourceFlow -> {
                    logHandler?.v("$TAG Use flow replayer location engine")
                    mapboxBuilder.locationEngine(FlowLocationEngine(it.flow, logHandler))
                }
            }
        }

        // Explanation from the Mapbox team: "By disabling this setting we strive for predictability and stability"
        InternalUtils.setUnconditionalPollingPatience(Long.MAX_VALUE)

        if (constantLocationEngineResolution != null) {
            mapboxBuilder.locationEngineRequest(constantLocationEngineResolution.toLocationEngineRequest())
        }

        runBlocking(mainDispatcher) {
            mapboxNavigation = MapboxInstanceProvider.createOrRetrieve(mapboxBuilder.build())
            logHandler?.v("$TAG obtained MapboxNavigation instance")
            setupRouteClearingWhenDestinationIsReached()
        }
    }

    private fun createDeviceProfile(vehicleProfile: VehicleProfile): DeviceProfile =
        DeviceProfile.Builder()
            .customConfig(
                when (vehicleProfile) {
                    VehicleProfile.CAR -> ASSET_TRACKING_PROFILE_DISABLED_CONFIGURATION
                    VehicleProfile.BICYCLE -> ASSET_TRACKING_PROFILE_ENABLED_CONFIGURATION
                }
            )
            .build()

    /**
     * On Android 24 and below the shared notification is removed when [MapboxNavigation.stopTripSession] is called.
     * This notification should be always visible when the [Publisher] is running so we show it once again manually.
     *
     * The permission POST_NOTIFICATIONS cannot be added as a RequiresPermission annotation as it was only added
     * in API level 33.
     */
    @SuppressLint("MissingPermission")
    private fun reshowTripNotification() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            NotificationManagerCompat.from(context).notify(notificationId, notificationProvider.getNotification())
        }
    }

    private fun setupTripNotification(notificationProvider: PublisherNotificationProvider, notificationId: Int) {
        Mapbox_TripNotificationModuleConfiguration.moduleProvider =
            object : Mapbox_TripNotificationModuleConfiguration.ModuleProvider {
                override fun createTripNotification(): TripNotification =
                    MapboxTripNotification(notificationProvider, notificationId)
            }
    }

    private fun setupRouteClearingWhenDestinationIsReached() {
        arrivalObserver = object : ArrivalObserver {
            override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                clearRoute()
            }

            override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) = Unit
            override fun onWaypointArrival(routeProgress: RouteProgress) = Unit
        }
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startTrip() {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Start trip and location updates")
            mapboxNavigation.apply {
                historyRecorder.startRecording()
                startTripSession()
            }
            mapboxReplayer?.play()
        }
    }

    override fun stopTrip() {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Stop trip and location updates")
            mapboxReplayer?.stop()
            mapboxReplayer?.seekTo(0.0)
            mapboxNavigation.stopTripSession()
            reshowTripNotification()
        }
    }

    override fun close() {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Close Mapbox")
            mapboxNavigation.apply {
                unregisterArrivalObserver(arrivalObserver)
                this@DefaultMapbox.mapboxReplayer?.finish()
                historyRecorder.stopRecording { historyDataFilepath ->
                    if (historyDataFilepath != null) {
                        val historyMapper = ReplayHistoryMapper.Builder().build()
                        val historyEvents = MapboxHistoryReader(historyDataFilepath)
                            .asSequence()
                            .mapNotNull { historyMapper.mapToReplayEvent(it) }
                            .toList()
                        locationHistoryListener?.invoke(LocationHistoryData(historyEvents.toGeoJsonMessages()))
                        rawHistoryCallback?.invoke(historyDataFilepath)
                    }
                }
            }

            if (MapboxInstanceProvider.destroyIfPossible()) {
                logHandler?.v("$TAG Destroyed the MapboxNavigation instance")
            }
        }
    }

    private fun createLocationObserver(locationUpdatesObserver: LocationUpdatesObserver) =
        mapboxLocationObserverProvider.createLocationObserver(locationUpdatesObserver)

    override fun registerLocationObserver(locationUpdatesObserver: LocationUpdatesObserver) {
        unregisterLocationObserver()
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Register location observer")
            locationObserver = createLocationObserver(locationUpdatesObserver)
            locationObserver?.let { mapboxNavigation.registerLocationObserver(it) }
        }
    }

    override fun unregisterLocationObserver() {
        locationObserver?.let {
            runBlocking(mainDispatcher) {
                logHandler?.v("$TAG Unregister location observer")
                mapboxNavigation.unregisterLocationObserver(it)
            }
        }
    }

    override fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: ResultCallbackFunction<Long>
    ) {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Set route to: $destination")
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .coordinatesList(getRouteCoordinates(currentLocation, destination))
                    .profile(routingProfile.toMapboxProfileName())
                    .build(),
                object : NavigationRouterCallback {
                    override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                        logHandler?.v("$TAG Set route successful")
                        routes.firstOrNull()?.let {
                            // According to the migration guide, we need to manually call [setRoutes] with routes list.
                            // https://docs.mapbox.com/android/navigation/guides/migrate-to-v2/#request-a-route
                            mapboxNavigation.setNavigationRoutes(routes)
                            val routeDuration = (it.directionsRoute.durationTypical() ?: it.directionsRoute.duration())
                            val routeDurationInMilliseconds =
                                routeDuration * MILLISECONDS_PER_SECOND
                            routeDurationCallback(Result.success(routeDurationInMilliseconds.toLong()))
                        }
                    }

                    override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) = Unit

                    override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                        // Use the exception from the reasons list if it is available
                        val throwable = reasons.firstOrNull()?.throwable ?: UnknownSetRouteException()
                        // We won't know the ETA for the active trackable and therefore we won't be able to check the temporal threshold.
                        routeDurationCallback(Result.failure(MapException(throwable)))
                        logHandler?.e("$TAG Set route failed", throwable)
                    }
                }
            )
        }
    }

    override fun clearRoute() {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Clear route")
            mapboxNavigation.setNavigationRoutes(emptyList())
        }
    }

    override fun changeResolution(resolution: Resolution) {
        runBlocking(mainDispatcher) {
            logHandler?.v("$TAG Change location engine resolution")
            mapboxNavigation.navigationOptions.locationEngine.let {
                if (it is ResolutionLocationEngine) {
                    it.changeResolution(resolution)
                }
            }
        }
    }

    private fun getBestLocationEngine(context: Context, logHandler: LogHandler?): ResolutionLocationEngine =
        if (LocationEngineUtils.hasGoogleLocationServices(context)) {
            logHandler?.v("$TAG Use Google location engine")
            GoogleLocationEngine(context)
        } else {
            logHandler?.v("$TAG Use Android location engine")
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

internal open class TimeProvider {
    open fun getCurrentTime() = System.currentTimeMillis()
}
