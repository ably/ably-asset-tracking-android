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
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
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
import timber.log.Timber

private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 500L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 10

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val connectionConfiguration: ConnectionConfiguration,
    private val mapConfiguration: MapConfiguration,
    private val debugConfiguration: DebugConfiguration?,
    private val locationUpdatedListener: LocationUpdatedListener,
    context: Context
) :
    Publisher {
    private val gson: Gson = Gson()
    private val mapboxNavigation: MapboxNavigation
    private val ably: AblyRealtime
    private var channel: Channel? = null
    private val locationEngingeCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            Timber.w("TestLocation ${result!!.lastLocation!!.latitude}")
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.e(exception)
        }
    }
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
    private var isTracking: Boolean = false
    private var mapboxReplayer: MapboxReplayer? = null
    private var lastKnownLocation: Location? = null

    /**
     * This field will be set only when trying to set a tracking destination before receiving any [lastKnownLocation].
     * After successfully setting the tracking destination this field will be set to NULL.
     **/
    private var destinationToSet: Destination? = null

    init {
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
        lastKnownLocation = rawLocation
        destinationToSet?.let { setDestination(it) }
        val geoJsonMessage = rawLocation.toGeoJson()
        Timber.d("sendRawLocationMessage: publishing: ${geoJsonMessage.synopsis()}")
        channel?.publish(EventNames.RAW, geoJsonMessage.toJsonArray(gson))
        locationUpdatedListener(rawLocation)
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        geoJsonMessages.forEach {
            Timber.d("sendEnhancedLocationMessage: publishing: ${it.synopsis()}")
        }
        channel?.publish(EventNames.ENHANCED, geoJsonMessages.toJsonArray(gson))
        locationUpdatedListener(enhancedLocation)
    }

    // TODO define threading strategy: https://github.com/ably/ably-asset-tracking-android/issues/22
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @Synchronized
    private fun startLocationUpdates() {
        if (!isTracking) {
            isTracking = true

            Timber.e("startLocationUpdates")

            mapboxNavigation.apply {
                toggleHistory(true)
                startTripSession()
            }

            postToMainThread {
                mapboxNavigation.navigationOptions.locationEngine.requestLocationUpdates(
                    LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                        .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                        .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                        .build(),
                    locationEngingeCallback,
                    getLooperForMainThread()
                )
            }
        }
    }

    override fun track(trackable: Trackable) {
        if (this.channel != null) {
            throw IllegalStateException("For this preview version of the SDK, this method may only be called once for any given instance of this class.")
        }

        channel = ably.channels.get(trackable.id).apply {
            try {
                presence.enterClient(
                    connectionConfiguration.clientId,
                    gson.toJson(presenceData),
                    object : CompletionListener {
                        override fun onSuccess() = Unit

                        override fun onError(reason: ErrorInfo?) {
                            // TODO - handle error
                            // https://github.com/ably/ably-asset-tracking-android/issues/17
                            Timber.e("Unable to enter presence: ${reason?.message}")
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                // TODO - handle exception
                // https://github.com/ably/ably-asset-tracking-android/issues/17
                Timber.e(ablyException)
            }
        }
        trackable.destination?.let { setDestination(it) }
    }

    override fun add(trackable: Trackable) {
        TODO("Not yet implemented")
    }

    override fun remove(trackable: Trackable): Boolean {
        if (trackable == active) {
            removeCurrentDestination()
        }
        TODO("Not yet implemented")
    }

    override val active: Trackable?
        get() = TODO("Not yet implemented")

    override var transportationMode: TransportationMode
        get() = TODO("Not yet implemented")
        set(value) {
            TODO("Not yet implemented")
        }

    override fun refreshResolution() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        stopLocationUpdates()
        ably.close()
    }

    // TODO define threading strategy: https://github.com/ably/ably-asset-tracking-android/issues/22
    @Synchronized
    private fun stopLocationUpdates() {
        if (isTracking) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            channel?.let {
                channel = null
                try {
                    it.presence.leaveClient(
                        connectionConfiguration.clientId,
                        gson.toJson(presenceData),
                        object : CompletionListener {
                            override fun onSuccess() = Unit

                            override fun onError(reason: ErrorInfo?) {
                                // TODO - handle error
                                // https://github.com/ably/ably-asset-tracking-android/issues/17
                                Timber.e("Unable to leave presence: ${reason?.message}")
                            }
                        }
                    )
                } catch (ablyException: AblyException) {
                    // TODO - handle exception
                    // https://github.com/ably/ably-asset-tracking-android/issues/17
                    Timber.e(ablyException)
                }
            }
            isTracking = false
            mapboxNavigation.navigationOptions.locationEngine.removeLocationUpdates(
                locationEngingeCallback
            )
            mapboxReplayer?.finish()
            debugConfiguration?.locationHistoryReadyListener?.invoke(mapboxNavigation.retrieveHistory())
            mapboxNavigation.apply {
                toggleHistory(false)
                toggleHistory(true)
            }
        }
    }

    // TODO define threading strategy: https://github.com/ably/ably-asset-tracking-android/issues/22
    private fun setDestination(destination: Destination) {
        lastKnownLocation.let { currentLocation ->
            if (currentLocation != null) {
                destinationToSet = null
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultParams()
                        .accessToken(mapConfiguration.apiKey)
                        .coordinates(getRouteCoordinates(currentLocation, destination))
                        .build()
                )
            } else {
                destinationToSet = destination
            }
        }
    }

    private fun getRouteCoordinates(
        startingLocation: Location,
        destination: Destination
    ): List<Point> = listOf(
        Point.fromLngLat(startingLocation.longitude, startingLocation.latitude),
        Point.fromLngLat(destination.longitude, destination.latitude)
    )

    private fun removeCurrentDestination() {
        mapboxNavigation.setRoutes(emptyList())
    }

    private fun postToMainThread(operation: () -> Unit) {
        Handler(getLooperForMainThread()).post(operation)
    }

    private fun getLooperForMainThread() = Looper.getMainLooper()
}
