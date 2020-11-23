package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import timber.log.Timber

private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 500L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 10

@SuppressLint("LogConditional")
internal class DefaultAssetPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val ablyConfiguration: AblyConfiguration,
    mapConfiguration: MapConfiguration,
    trackingId: String,
    private val locationUpdatedListener: LocationUpdatedListener,
    context: Context
) :
    AssetPublisher {
    private val gson: Gson = Gson()
    private val mapboxNavigation: MapboxNavigation
    private val ably: AblyRealtime
    private val channel: Channel
    private val locationEngingeCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            Timber.w("TestLocation ${result!!.lastLocation!!.latitude}")
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.e(exception)
        }
    }
    private var isTracking: Boolean = false

    init {
        ably = AblyRealtime(ablyConfiguration.apiKey)
        channel = ably.channels.get(trackingId)

        Timber.w("Started.")

        mapboxNavigation = MapboxNavigation(
            MapboxNavigation.defaultNavigationOptionsBuilder(context, mapConfiguration.apiKey)
                .build()
        )
        setupLocationUpdatesListener()
        startLocationUpdates()
    }

    private fun setupLocationUpdatesListener() {
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onRawLocationChanged(rawLocation: Location) {
                sendRawLocationMessage(rawLocation)
            }

            override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
            ) {
                sendEnhancedLocationMessage(enhancedLocation, keyPoints)
            }
        })
    }

    private fun sendRawLocationMessage(rawLocation: Location) {
        val geoJsonMessage = rawLocation.toGeoJson()
        Timber.d("sendRawLocationMessage: publishing: ${geoJsonMessage.synopsis()}")
        channel.publish("raw", geoJsonMessage.toJsonArray(gson))
        locationUpdatedListener(rawLocation)
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        geoJsonMessages.forEach {
            Timber.d("sendEnhancedLocationMessage: publishing: ${it.synopsis()}")
        }
        channel.publish("enhanced", geoJsonMessages.toJsonArray(gson))
        locationUpdatedListener(enhancedLocation)
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @Synchronized
    private fun startLocationUpdates() {
        if (!isTracking) {
            isTracking = true

            try {
                channel.presence.enterClient(
                    ablyConfiguration.clientId, "",
                    object : CompletionListener {
                        override fun onSuccess() = Unit

                        override fun onError(reason: ErrorInfo?) {
                            // TODO - handle error
                            // https://github.com/ably/ably-asset-tracking-android/issues/17
                            Timber.e("Unable to enter presence")
                        }
                    }
                )
            } catch (e: AblyException) {
                // TODO - handle exception
                // https://github.com/ably/ably-asset-tracking-android/issues/17
                e.printStackTrace()
            }

            Timber.e("startLocationUpdates")

            mapboxNavigation.apply {
                toggleHistory(true)
                startTripSession()
            }

            // TODO: this is involves the main thread, needs to be checked for running in the background
            // https://github.com/ably/ably-asset-tracking-android/issues/18
            mapboxNavigation.navigationOptions.locationEngine.requestLocationUpdates(
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                    .build(),
                locationEngingeCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun stop() {
        stopLocationUpdates()
        ably.close()
    }

    @Synchronized
    private fun stopLocationUpdates() {
        if (isTracking) {
            try {
                channel.presence.leaveClient(
                    ablyConfiguration.clientId, "",
                    object : CompletionListener {
                        override fun onSuccess() = Unit

                        override fun onError(reason: ErrorInfo?) {
                            // TODO - handle error
                            // https://github.com/ably/ably-asset-tracking-android/issues/17
                            Timber.e("Unable to leave presence")
                        }
                    }
                )
            } catch (e: AblyException) {
                // TODO - handle exception
                // https://github.com/ably/ably-asset-tracking-android/issues/17
                e.printStackTrace()
            }
            isTracking = false
            mapboxNavigation.apply {
                navigationOptions.locationEngine.removeLocationUpdates(locationEngingeCallback)
                toggleHistory(false)
                toggleHistory(true)
            }
        }
    }
}
