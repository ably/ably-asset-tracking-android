package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.Logging.testLogD
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.createNotificationChannel
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class PublisherIntegrationTests {
    val gson = Gson()

    @Test
    fun createAndStartPublisherAndWaitUntilDataEnds() {
        testLogD("##########  PublisherIntegrationTests.createAndStartPublisherAndWaitUntilDataEnds  ##########")

        // given
        testLogD("GIVEN")
        val dataEndedExpectation = UnitExpectation("data ended")
        val trackExpectation = BooleanExpectation("track response")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationData = getLocationData(context)
        createNotificationChannel(context)

        // when
        testLogD("WHEN")
        val publisher = createAndStartPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = {
                testLogD("data ended")
                dataEndedExpectation.fulfill()
            }
        )
        runBlocking {
            try {
                publisher.track(Trackable(UUID.randomUUID().toString()))
                testLogD("track success")
                trackExpectation.fulfill(true)
            } catch (e: Exception) {
                testLogD("track failed")
                trackExpectation.fulfill(false)
            }
        }

        // await asynchronous events
        testLogD("AWAIT")
        dataEndedExpectation.await()
        trackExpectation.await()

        // cleanup
        testLogD("CLEANUP")
        val stopExpectation = BooleanExpectation("stop response")
        runBlocking {
            try {
                publisher.stop()
                testLogD("stop success")
                stopExpectation.fulfill(true)
            } catch (e: Exception) {
                testLogD("stop failed")
                stopExpectation.fulfill(true)
            }
        }
        stopExpectation.await()

        // then
        testLogD("THEN")
        dataEndedExpectation.assertFulfilled()
        trackExpectation.assertSuccess()
        stopExpectation.assertSuccess()
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartPublisher(
        context: Context,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0),
        locationData: LocationHistoryData,
        onLocationDataEnded: () -> Unit
    ) =
        Publisher.publishers()
            .androidContext(context)
            .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
            .profile(RoutingProfile.CYCLING)
            .locationSource(LocationSourceRaw.create(locationData, onLocationDataEnded))
            .backgroundTrackingNotificationProvider(
                object : PublisherNotificationProvider {
                    override fun getNotification(): Notification =
                        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("TEST")
                            .setContentText("Test")
                            .setSmallIcon(R.drawable.aat_logo)
                            .build()
                },
                12345
            )
            .start()

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }
}
