package com.ably.tracking.publisher

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.test.android.common.*
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions
import io.ably.lib.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

private const val AGENT_HEADER_NAME = "ably-asset-tracking-android-publisher-tests"


@RunWith(AndroidJUnit4::class)
class NetworkConnectivityTests {

    val gson = Gson()

    @Test
    fun createAndStartPublishingNormalConnectivity() {
        testLogD("#### NetworkConnectivityTests.createAndStartPublishingNormalConnectivity ####")

        // given
        testLogD("GIVEN")
        val dataEndedExpectation = UnitExpectation("data ended")
        val trackExpectation = BooleanExpectation("track response")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationData = getLocationData(context)
        createNotificationChannel(context)

        // when
        testLogD("WHEN")
        val publisher = createPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = {
                testLogD("data ended")
                dataEndedExpectation.fulfill()
            },
            realtime = realtimeClient()
        )


        runBlocking {
            try {
                publisher.track(Trackable("ID"))
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

    private val aatDebugLogger = object : LogHandler {
        override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
            if (throwable != null) {
                testLogD("$message $throwable")
            } else {
                testLogD(message)
            }
        }
    }

    private val ablyJavaDebugLogger = Log.LogHandler {
            _, _, msg, tr -> aatDebugLogger.logMessage(LogLevel.DEBUG, msg!!, tr)
    }


    private fun realtimeClient() : AblyRealtime {
        val clientOptions = ClientOptions().apply {
            this.clientId = CLIENT_ID
            this.agents = mapOf(AGENT_HEADER_NAME to com.ably.tracking.common.BuildConfig.VERSION_NAME)
            this.idempotentRestPublishing = true
            this.autoConnect = false
            this.key = ABLY_API_KEY
            this.logHandler = ablyJavaDebugLogger
        }
        return AblyRealtime(clientOptions)
    }

    private fun createPublisher(
        context: Context,
        realtime: AblyRealtime,
        locationData: LocationHistoryData,
        onLocationDataEnded: () -> Unit
    ) : Publisher {
        val resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0)
        return DefaultPublisher(
            DefaultAbly(realtime, null),
            DefaultMapbox(
                context,
                MapConfiguration(MAPBOX_ACCESS_TOKEN),
                ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
                LocationSourceRaw.create(locationData, onLocationDataEnded),
                null,
                object : PublisherNotificationProvider {
                    override fun getNotification(): Notification =
                        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("TEST")
                            .setContentText("Test")
                            .setSmallIcon(R.drawable.aat_logo)
                            .build()
                },
                notificationId = 12345,
                rawHistoryCallback = null,
                resolution,
                VehicleProfile.BICYCLE
            ),
            DefaultResolutionPolicyFactory(resolution, context),
            RoutingProfile.CYCLING,
            aatDebugLogger,
            areRawLocationsEnabled = true,
            sendResolutionEnabled = true,
            resolution
        )
    }

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }

}
