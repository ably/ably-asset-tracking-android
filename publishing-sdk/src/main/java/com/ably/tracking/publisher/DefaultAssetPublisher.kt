package com.ably.tracking.publisher

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import timber.log.Timber

internal class DefaultAssetPublisher(
    ablyConfiguration: AblyConfiguration,
    mapConfiguration: MapConfiguration,
    trackingId: String,
    private val locationUpdatedListener: LocationUpdatedListener,
    context: Context
) :
    AssetPublisher {
    private val TAG: String = DefaultAssetPublisher::class.java.simpleName
    private val gson: Gson = Gson()
    private val mapboxNavigation: MapboxNavigation
    private val ably: AblyRealtime
    private val channel: Channel

    init {
        ably = AblyRealtime(ablyConfiguration.apiKey)
        channel = ably.channels.get(trackingId)

        Timber.w(TAG, "Started.")

        mapboxNavigation = MapboxNavigation(
            MapboxNavigation.defaultNavigationOptionsBuilder(context, mapConfiguration.apiKey)
                .build()
        )
        setupLocationUpdatesListener()
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
        Timber.d(TAG, "sendRawLocationMessage: publishing: ${geoJsonMessage.synopsis()}")
        channel.publish("raw", geoJsonMessage.toJsonArray(gson))
        locationUpdatedListener(rawLocation)
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        geoJsonMessages.forEach {
            Timber.d(TAG, "sendEnhancedLocationMessage: publishing: ${it.synopsis()}")
        }
        channel.publish("enhanced", geoJsonMessages.toJsonArray(gson))
        locationUpdatedListener(enhancedLocation)
    }

    override fun stop() {
        TODO()
    }
}
