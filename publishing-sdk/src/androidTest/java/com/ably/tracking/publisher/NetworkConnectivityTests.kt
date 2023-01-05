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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.reflect.KClass

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN

/**
 * Unfortunately, state transitions need to wait for SDK retry loops, some of which
 * have a 15s pause. Setting this to two attempts should mean that the test has a chance
 * to pass even if it just missed a retry before listening.
 */
private const val DEFAULT_STATE_TRANSITION_TIMEOUT_SECONDS = 30L

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

private val DEFAULT_CLIENT_OPTS = ClientOptions().apply {
    this.clientId = "IntegTests_NoProxy"
    this.key = BuildConfig.ABLY_API_KEY
    this.logHandler = Logging.ablyJavaDebugLogger
}


/**
 * Helper class to publish basic location updates through a known Ably channel name
 */
class LocationHelper(
    trackableId: String
) {
    private val opts = DEFAULT_CLIENT_OPTS
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
        // (1)
        val trackableId = UUID.randomUUID().toString()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope =  CoroutineScope(Dispatchers.Unconfined)
        val locationHelper = LocationHelper(trackableId)
        val fault = NullTransportFault()
        fault.proxy.start()

        createNotificationChannel(context!!)

        val publisher = createPublisher(
            context,
            fault.proxy.clientOptions,
            locationHelper.channelName
        )

        // (2, 3, 4)
        trackNewTrackable(trackableId, publisher, locationHelper, scope)

        // (5)
        scope.cancel()
        val stopExpectation = shutdownPublisher(publisher)
        stopExpectation.assertSuccess()
        locationHelper.close()
        fault.proxy.stop()
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
        testLogD("#### NetworkConnectivityTests.resumesPublishingAfterInterruptedConnectivity ####")
        // (1)
        val trackableId = UUID.randomUUID().toString()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope =  CoroutineScope(Dispatchers.Unconfined)
        val locationHelper = LocationHelper(trackableId)
        val fault = TcpConnectionRefused()
        fault.proxy.start()

        createNotificationChannel(context!!)

        val publisher = createPublisher(
            context,
            fault.proxy.clientOptions,
            locationHelper.channelName
        )


        // (2, 3, 4)
        trackNewTrackable(trackableId, publisher, locationHelper, scope)

        // (5, 6, 7)
        waitForStateTransition(
            actionLabel = "terminate proxy and publish another location update",
            receiver = TrackableStateReceiver(
                label = "trackable offline when interrupted",
                expectedStates = setOf(TrackableState.Offline::class),
                failureStates = setOf(TrackableState.Failed::class)
            ),
            scope = scope
        ) {
            fault.enable()
            locationHelper.sendUpdate(12.0, 13.0)
            publisher.getTrackableState(trackableId)!!
        }

        // (8, 9, 10)
        waitForStateTransition(
            actionLabel = "restore connection and publish an update",
            receiver = TrackableStateReceiver(
                label = "trackable reconnected",
                expectedStates = setOf(TrackableState.Online::class),
                failureStates = setOf(TrackableState.Failed::class)
            ),
            scope = scope
        ) {
            fault.resolve()
            locationHelper.sendUpdate(14.0, 15.0)
            publisher.getTrackableState(trackableId)!!
        }

        // (11)
        scope.cancel()
        val stopExpectation = shutdownPublisher(publisher)
        stopExpectation.assertSuccess()
        locationHelper.close()
        fault.proxy.stop()
    }

    /**
     * Helper to initiate tracking of a new Trackable and assert that it reaches the Online
     * state initially.
     */
    private fun trackNewTrackable(
        trackableId: String,
        publisher: Publisher,
        locationHelper: LocationHelper,
        scope: CoroutineScope,
    ) {
        waitForStateTransition(
            actionLabel = "track new Trackable($trackableId)",
            receiver = TrackableStateReceiver(
                label = "trackable online",
                expectedStates = setOf(TrackableState.Online::class),
                failureStates = setOf(TrackableState.Failed::class)
            ),
            scope = scope
        ) {
            publisher.track(Trackable(trackableId)).also {
                locationHelper.sendUpdate(10.0, 11.0)
            }
        }
    }

    /**
     * Performs the given async (suspending) operation in a runBlocking, attaching the
     * returned StateFlow<TrackableState> to the given receiver, then waits for expectations
     * to be delivered (or not) before cleaning up.
     */
    private fun waitForStateTransition(
        actionLabel: String,
        receiver: TrackableStateReceiver,
        scope: CoroutineScope,
        asyncOp: suspend () -> StateFlow<TrackableState>
    ) {
        var job: Job? = null
        val completedExpectation = failOnException(actionLabel) {
            job = asyncOp().onEach(receiver::receive).launchIn(scope)
        }

        completedExpectation.await()
        completedExpectation.assertSuccess()
        receiver.outcome.await(DEFAULT_STATE_TRANSITION_TIMEOUT_SECONDS)
        receiver.outcome.assertSuccess()
        runBlocking {
            job?.cancelAndJoin()
        }
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
            } catch (e: Exception) {
                testLogD("$label - failed - $e")
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
     * Injects a pre-configured AblyRealtime instance to the Publisher by constructing it
     * and all dependencies by hand, side-stepping the builders, which block this.
     */
    private fun createPublisher(
        context: Context,
        clientOptions: ClientOptions,
        locationChannelName: String
    ) : Publisher {
        val resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0)
        return DefaultPublisher(
            DefaultAbly(AblyRealtime(clientOptions), null),
            DefaultMapbox(
                context,
                MapConfiguration(MAPBOX_ACCESS_TOKEN),
                ConnectionConfiguration(
                    Authentication.basic(
                        clientOptions.clientId,
                        clientOptions.key
                    )
                ),
                LocationSourceAbly.create(locationChannelName, DEFAULT_CLIENT_OPTS),
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
