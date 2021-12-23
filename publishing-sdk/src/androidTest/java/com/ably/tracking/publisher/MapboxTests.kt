package com.ably.tracking.publisher

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.ably.tracking.test.android.common.createNotificationChannel
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class MapboxTests {
    private val gson = Gson()

    @Test
    fun shouldNotThrowErrorWhenMapboxIsStartedAndStoppedWithoutStartingTrip() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createNotificationChannel(context)
        val mapbox: Mapbox = createMapbox(context)

        // when
        mapbox.stopAndClose()

        // then
    }

    private fun createMapbox(context: Context) =
        DefaultMapbox(
            context,
            MapConfiguration(MAPBOX_ACCESS_TOKEN),
            ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
            LocationSourceRaw.create(getLocationData(context), null),
            null,
            object : PublisherNotificationProvider {
                override fun getNotification(): Notification =
                    NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("TEST")
                        .setContentText("Test")
                        .setSmallIcon(R.drawable.aat_logo)
                        .build()
            },
            12345,
            true,
            null,
        )

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }
}
