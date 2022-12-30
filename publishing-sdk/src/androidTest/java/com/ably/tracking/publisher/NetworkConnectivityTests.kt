package com.ably.tracking.publisher

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.message.LocationGeometry
import com.ably.tracking.common.message.LocationMessage
import com.ably.tracking.common.message.LocationProperties
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.test.android.common.*
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

private const val AGENT_HEADER_NAME = "ably-asset-tracking-android-publisher-tests"

private const val PROXY_HOST = "localhost"
private const val PROXY_PORT = 13579
private const val REALTIME_HOST = "realtime.ably.io"
private const val REALTIME_PORT = 443

/**
 * Redirect Ably and AAT logging to Log.d
 */
object Logging {
    val aatDebugLogger = object : LogHandler {
        override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
            if (throwable != null) {
                testLogD("$message $throwable")
            } else {
                testLogD(message)
            }
        }
    }

    val ablyJavaDebugLogger = Log.LogHandler { _, _, msg, tr ->
        aatDebugLogger.logMessage(LogLevel.DEBUG, msg!!, tr)
    }
}

/**
 * Helper class to publish basic location updates through a known Ably channel name
 */
class LocationHelper(
    trackableId: String
) {
    private val opts = ClientOptions().apply {
        this.clientId = "IntegTests_LocationHelper"
        this.key = ABLY_API_KEY
        this.logHandler = Logging.ablyJavaDebugLogger
    }

    private val gson = Gson()
    private val ably = AblyRealtime(opts)
    val channelName = "locations:$trackableId"
    private val channel = ably.channels.get(channelName)

    /**
     * Send a location update message on trackable channel and wait for confirmation
     * of publish completing successfully. Will fail the test if publishing fails.
     */
    fun sendUpdate(lat: Double, long: Double) {
        val geoJson = LocationMessage(
            type = "Feature",
            geometry = LocationGeometry(
                type = "Point",
                coordinates = listOf(lat, long, 0.0)
            ),
            properties = LocationProperties(
                accuracyHorizontal = 5.0f,
                bearing = 0.0f,
                speed = 5.0f,
                time = Date().time.toDouble() / 1000
            )
        )

        val ablyMessage = Message(EventNames.ENHANCED, gson.toJson(arrayOf((geoJson))))
        val publishExpectation = BooleanExpectation("publishing Ably location update")
        channel.publish(ablyMessage, object: CompletionListener {
            override fun onSuccess() {
                testLogD("Location publish success")
                publishExpectation.fulfill(true)
            }
            override fun onError(err: ErrorInfo?) {
                testLogD("Location publish failed: ${err?.code} - ${err?.message}")
                publishExpectation.fulfill(false)
            }
        })

        publishExpectation.await()
        publishExpectation.assertSuccess()
    }
}

@RunWith(AndroidJUnit4::class)
class NetworkConnectivityTests {


    @Test
    fun createAndStartPublishingNormalConnectivity() {
        testLogD("#### NetworkConnectivityTests.createAndStartPublishingNormalConnectivity ####")

        // given
        testLogD("GIVEN")
        val trackableId = "123abc"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationHelper = LocationHelper(trackableId)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        createNotificationChannel(context)

        // proxy
        val proxy = AblyProxy(PROXY_PORT, REALTIME_HOST, REALTIME_PORT) { s: String -> testLogD(s) }
        proxy.start()

        // when
        testLogD("WHEN")
        val publisher = createPublisher(
            context,
            realtime = AblyRealtime(proxiedClientOptions()),
            locationHelper.channelName
        )

        val trackExpectation = BooleanExpectation("track response")
        val connectionSuccessExpectation = BooleanExpectation("trackable connected")
        runBlocking {
            try {
                publisher.track(Trackable(trackableId)).onEach {
                    testLogD("JALEY: $it")
                    when(it) {
                        is TrackableState.Failed -> connectionSuccessExpectation.fulfill(false)
                        is TrackableState.Offline -> {}
                        TrackableState.Online -> connectionSuccessExpectation.fulfill(true)
                        TrackableState.Publishing -> {}
                    }
                }.launchIn(scope)

                testLogD("track success")
                trackExpectation.fulfill(true)
                // locationHelper.sendUpdate(50.0, 50.0)
            } catch (e: Exception) {
                testLogD("track failed")
                trackExpectation.fulfill(false)
            }
        }

        locationHelper.sendUpdate(10.0, 11.0)

        // await asynchronous events
        testLogD("AWAIT")
        trackExpectation.await()
        connectionSuccessExpectation.await(10L)
        connectionSuccessExpectation.assertSuccess()

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
        trackExpectation.assertSuccess()
        stopExpectation.assertSuccess()
    }

    private fun proxiedClientOptions() = ClientOptions().apply {
        this.clientId = CLIENT_ID
        this.agents = mapOf(AGENT_HEADER_NAME to com.ably.tracking.common.BuildConfig.VERSION_NAME)
        this.idempotentRestPublishing = true
        this.autoConnect = false
        this.key = ABLY_API_KEY
        this.logHandler = Logging.ablyJavaDebugLogger
        this.realtimeHost = PROXY_HOST
        this.port = PROXY_PORT
        this.tls = false
    }

    private fun createPublisher(
        context: Context,
        realtime: AblyRealtime,
        locationChannelName: String
    ) : Publisher {
        val resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0)
        return DefaultPublisher(
            DefaultAbly(realtime, null),
            DefaultMapbox(
                context,
                MapConfiguration(MAPBOX_ACCESS_TOKEN),
                ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
                LocationSourceAbly.create(locationChannelName),
                logHandler = Logging.aatDebugLogger,
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
            Logging.aatDebugLogger,
            areRawLocationsEnabled = true,
            sendResolutionEnabled = true,
            resolution
        )
    }

}
