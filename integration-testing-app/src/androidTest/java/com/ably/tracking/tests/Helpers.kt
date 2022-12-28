package com.ably.tracking.tests

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.PublisherNotificationProvider
import com.ably.tracking.publisher.RoutingProfile
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
    locationData: LocationHistoryData = getLocationData(context),
    rawLocations: Boolean = false,
    sendResolution: Boolean = true,
    onLocationDataEnded: () -> Unit = {}
): Publisher {
    createNotificationChannel(context)
    return Publisher.publishers()
        .androidContext(context)
        .connection(ConnectionConfiguration(authentication))
        .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
        .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
        .profile(RoutingProfile.DRIVING)
        .locationSource(LocationSourceRaw.create(locationData, onLocationDataEnded))
        .backgroundTrackingNotificationProvider(
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
        .rawLocations(rawLocations)
        .sendResolution(sendResolution)
        .start()
}

suspend fun createAndStartSubscriber(
    trackingId: String,
    resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0),
    authentication: Authentication = defaultConnectionConfiguration,
) =
    Subscriber.subscribers()
        .connection(ConnectionConfiguration(authentication))
        .resolution(resolution)
        .trackingId(trackingId)
        .logHandler()
        .start()

private fun getLocationData(context: Context): LocationHistoryData {
    val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
    return Gson().fromJson(historyString, LocationHistoryData::class.java)
}
