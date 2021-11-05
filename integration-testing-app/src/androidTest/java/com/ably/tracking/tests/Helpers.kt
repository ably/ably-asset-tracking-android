package com.ably.tracking.tests

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.locationprovider.RoutingProfile
import com.ably.tracking.locationprovider.mapbox.LocationSourceRaw
import com.ably.tracking.locationprovider.mapbox.MapConfiguration
import com.ably.tracking.locationprovider.mapbox.MapboxLocationHistoryData
import com.ably.tracking.locationprovider.mapbox.MapboxLocationProvider
import com.ably.tracking.locationprovider.mapbox.PublisherNotificationProvider
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.ably.tracking.test.android.common.createNotificationChannel
import com.google.gson.Gson

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
const val CLIENT_ID = "IntegrationTestsClient"
const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

private val defaultConnectionConfiguration = Authentication.basic(CLIENT_ID, ABLY_API_KEY)

@SuppressLint("MissingPermission")
fun createAndStartPublisher(
    context: Context,
    resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0),
    authentication: Authentication = defaultConnectionConfiguration,
    locationData: MapboxLocationHistoryData = getLocationData(context),
    onLocationDataEnded: () -> Unit = {}
): Publisher {
    return Publisher.publishers()
        .connection(ConnectionConfiguration(authentication))
        .locationProvider(createMapboxLocationProvider(context, authentication, locationData, onLocationDataEnded))
        .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
        .profile(RoutingProfile.DRIVING)
        .start()
}

suspend fun createAndStartSubscriber(
    trackingId: String,
    resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0),
    authentication: Authentication = defaultConnectionConfiguration
) =
    Subscriber.subscribers()
        .connection(ConnectionConfiguration(authentication))
        .resolution(resolution)
        .trackingId(trackingId)
        .start()

fun createMapboxLocationProvider(
    context: Context,
    authentication: Authentication = defaultConnectionConfiguration,
    locationData: MapboxLocationHistoryData = getLocationData(context),
    onLocationDataEnded: () -> Unit = {},
): MapboxLocationProvider {
    createNotificationChannel(context)
    return MapboxLocationProvider(
        context,
        MapConfiguration(MAPBOX_ACCESS_TOKEN),
        ConnectionConfiguration(authentication),
        LocationSourceRaw.create(locationData, onLocationDataEnded),
        null,
        object : PublisherNotificationProvider {
            override fun getNotification(): Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Title")
                    .setContentText("Text")
                    .setSmallIcon(R.drawable.aat_logo)
                    .build()
        },
        1234
    )
}

private fun getLocationData(context: Context): MapboxLocationHistoryData {
    val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
    return Gson().fromJson(historyString, MapboxLocationHistoryData::class.java)
}
