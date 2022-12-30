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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.reflect.KClass

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
    private val ably = AblyRealtime(opts)

    val channelName = "locations:$trackableId"
    private val channel = ably.channels.get(channelName)

    private val gson = Gson()

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

    /**
     * Close Ably connection
     */
    fun close() {
        ably.close()
    }
}

/**
 * Helper to capture an expected set of successful or unsuccessful TrackableState
 * transitions using the StateFlows provided by publishers.
 */
class TrackableStateReceiver(
    private val label: String,
    private val expectedStates: Set<KClass<out TrackableState>>,
    private val failureStates: Set<KClass<out TrackableState>>
) {
    val outcome = BooleanExpectation(label)

    fun receive(state: TrackableState) {
        if (failureStates.contains((state::class))) {
            testLogD("TrackableStateReceived (FAIL): $label - $state")
            outcome.fulfill(false)
        } else if (expectedStates.contains(state::class)) {
            testLogD("TrackableStateReceived (SUCCESS): $label - $state")
            outcome.fulfill(true)
        } else {
            testLogD("TrackableStateReceived (IGNORED): $label - $state")
        }
    }
}

@RunWith(AndroidJUnit4::class)
class NetworkConnectivityTests {

    // Common test resources
    private var trackableId: String? = null
    private var context : Context? = null
    private var scope : CoroutineScope? = null
    private var locationHelper : LocationHelper? = null
    private var proxy : AblyProxy? = null
    private var publisher : Publisher? = null

    @Before
    fun setUp() {
        trackableId = UUID.randomUUID().toString()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        scope =  CoroutineScope(Dispatchers.Unconfined)
        locationHelper = LocationHelper(trackableId!!)
        createNotificationChannel(context!!)

        proxy = AblyProxy(PROXY_PORT, REALTIME_HOST, REALTIME_PORT) { s: String -> testLogD(s) }
        proxy?.start()

        publisher = createPublisher(
            context!!,
            AblyRealtime(proxiedClientOptions()),
            locationHelper!!.channelName
        )
    }

    @After
    fun tearDown() {
        scope?.cancel()
        val stopExpectation = shutdownPublisher(publisher!!)
        stopExpectation.assertSuccess()
        proxy?.close()
        locationHelper?.close()
    }

    /**
     * This test ensures that attempts to track a trackable under normal network conditions
     * are working, ensuring the Proxy isn't causing unintended problems.
     *
     * 1. Configure Publisher to use Ably through AblyProxy
     * 2. Attempt to track a new Trackable
     * 3. Trigger a location update
     * 4. Ensure Trackable state is now Online
     * 5. Shutdown publisher
     */
    @Test
    fun publishesTrackableStateThroughStableProxy() {
        testLogD("#### NetworkConnectivityTests.publishesTrackableStateThroughStableProxy ####")
        trackNewTrackable(trackableId!!, publisher!!, locationHelper!!)
    }

    /**
     *
     *  Test that a Trackable moves to offline state when connection is broken,
     *  then returns to the Online state when connectivity is restored.
     *
     * 1.  Configure Publisher to use Ably through proxy
     * 2.  Attempt to track a new Trackable
     * 3.  Publish a location update
     * 4.  Ensure Trackable is initially Online
     * 5.  Interrupt proxy connection
     * 6.  Publish another location update
     * 7.  Ensure Trackable is now Offline
     * 8.  Restore proxy connectivity
     * 9.  Publish another location update
     * 10. Ensure Trackable is finally Online again
     * 11. Shutdown publisher
     */
    @Test
    fun resumesPublishingAfterInterruptedConnectivity() {
        testLogD("#### NetworkConnectivityTests.publishesTrackableStateThroughStableProxy ####")

        trackNewTrackable(trackableId!!, publisher!!, locationHelper!!)
    }

    /**
     * Helper to initiate tracking of a new Trackable and assert that it reaches the Online
     * state initially.
     */
    private fun trackNewTrackable(
        trackableId: String,
        publisher: Publisher,
        locationHelper: LocationHelper
    ) {
        val connectedReceiver = TrackableStateReceiver(
            "trackable online",
            expectedStates = setOf(TrackableState.Online::class),
            failureStates = setOf(TrackableState.Failed::class)
        )

        val trackExpectation = failOnException("track new Trackable($trackableId)") {
            publisher.track(Trackable(trackableId))
                .onEach(connectedReceiver::receive)
                .launchIn(scope!!)
            locationHelper.sendUpdate(10.0, 11.0)
        }

        // await asynchronous events
        testLogD("AWAIT")
        trackExpectation.await()
        trackExpectation.assertSuccess()
        connectedReceiver.outcome.await()
        connectedReceiver.outcome.assertSuccess()
    }

    /**
     * Run the (suspending) async operation in a runBlocking and capture any exceptions that
     * occur. A BooleanExpectation is returned, which will be completed with success if asyncOp
     * completes without errors, or failed if an exception is thrown.
     */
    private fun failOnException(label: String, asyncOp: suspend () -> Unit) : BooleanExpectation {
        val opCompleted = BooleanExpectation(label)
        runBlocking {
            try {
                asyncOp()
                testLogD("$label - success")
                opCompleted.fulfill(true)
            } catch (e: java.lang.Exception) {
                testLogD("$label - failed - ${e.toString()}")
                opCompleted.fulfill(false)
            }
        }
        return opCompleted
    }

    /**
     * Shutdown the given publisher and wait for confirmation, or a timeout.
     * Returns a BooleanExpectation, which can be used to check for successful
     * shutdown of the publisher
     */
    private fun shutdownPublisher(publisher: Publisher) : BooleanExpectation {
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
        return stopExpectation
    }

    /**
     * Returns AblyRealtime ClientOptions that are configured to direct traffic through
     * a local proxy server using an insecure connection, so that messages can be
     * intercepted and faked for testing purposes.
     */
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

    /**
     * Injects a pre-configured AblyRealtime instance to the Publisher by constructing it
     * and all dependencies by hand, side-stepping the builders, which block this.
     */
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
            resolutionPolicyFactory = DefaultResolutionPolicyFactory(resolution, context),
            routingProfile = RoutingProfile.CYCLING,
            logHandler = Logging.aatDebugLogger,
            areRawLocationsEnabled = true,
            sendResolutionEnabled = true,
            constantLocationEngineResolution = resolution
        )
    }

}
