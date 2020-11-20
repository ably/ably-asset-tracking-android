package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel

@SuppressLint("LogConditional", "LogNotTimber")
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

        Log.w(TAG, "AblyNav: started!")

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
        Log.d(TAG, "onRawLocationChanged: publishing: ${geoJsonMessage.synopsis()}")
        channel.publish("raw", geoJsonMessage.toJsonArray(gson))
        locationUpdatedListener(rawLocation)
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        geoJsonMessages.forEach {
            Log.d(TAG, "onEnhancedLocationChanged: publishing: ${it.synopsis()}")
        }
        channel.publish("enhanced", geoJsonMessages.toJsonArray(gson))
        locationUpdatedListener(enhancedLocation)
    }

    override fun stop() {
        TODO()
    }
}
