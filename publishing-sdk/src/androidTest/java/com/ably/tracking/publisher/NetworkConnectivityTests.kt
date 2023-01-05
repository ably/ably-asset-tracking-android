package com.ably.tracking.publisher

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN

/**
 * Unfortunately, state transitions need to wait for SDK retry loops, some of which
 * have a 15s pause. Setting this to two attempts should mean that the test has a chance
 * to pass even if it just missed a retry before listening.
 */
private const val DEFAULT_STATE_TRANSITION_TIMEOUT_SECONDS = 30L


@RunWith(Parameterized::class)
class NetworkConnectivityTests(private val testFault: FaultSimulation) {

    private var testResources: TestResources? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(NullTransportFault()),
            arrayOf(TcpConnectionRefused())
        )
    }

    @Before
    fun setUp() {
        testResources = TestResources.setUp(testFault)
        createNotificationChannel(testResources?.context!!)
    }

    @After
    fun tearDown() {
        testResources?.tearDown()
    }

    /**
     * Test that Publisher can handle the given fault occurring before a user
     * adds a new trackable, and moves Trackables to the expected state once
     * the fault has cleared.
     */
    @Test
    fun faultBeforeAddingTrackable() {
        withResources { resources ->
            resources.fault.enable()
            waitForStateTransition(
                actionLabel = "attempt to add Trackable while fault active",
                receiver = resources.fault.stateReceiverForStage(FaultSimulationStage.FaultActive)
            ) {
                resources.publisher.track(
                    Trackable(resources.activeTracklableId)
                ).also {
                    resources.locationHelper.sendUpdate(100.0, 100.0)
                }
            }

            resources.fault.resolve()
            waitForStateTransition(
                actionLabel = "resolve fault and wait for updated state",
                receiver = resources.fault.stateReceiverForStage(FaultSimulationStage.FaultResolved)
            ) {
                resources.publisher.getTrackableState(resources.activeTracklableId)!!
            }
        }
    }

    /**
     * Tests that tracking of a Trackable recovers if a connectivity fault
     * occurs after a Trackable has been added and already reached the
     * Online state.
     */
    @Test
    fun faultDuringTracking() {
        withResources { resources ->
            // Add trackable, wait for it to reach Online state
            waitForStateTransition(
                actionLabel = "add new Trackables with working connectivity",
                receiver = TrackableStateReceiver.onlineWithoutFail(
                    "new trackable reaches online state"
                )
            ) {
                resources.publisher.track(
                    Trackable(resources.activeTracklableId)
                ).also {
                    resources.locationHelper.sendUpdate(101.0, 101.0)
                }
            }

            // Enable the fault, wait for Trackable to move to expected state
            waitForStateTransition(
                actionLabel = "monitor state transition during fault",
                receiver = resources.fault.stateReceiverForStage(FaultSimulationStage.FaultActive)
            ) {
                resources.fault.enable()
                resources.publisher.getTrackableState(
                    resources.activeTracklableId
                )!!
            }

            // Resolve the fault, wait for Trackable to move to expected state
            waitForStateTransition(
                actionLabel = "resolve fault and wait for transition",
                receiver = resources.fault.stateReceiverForStage(FaultSimulationStage.FaultResolved)
            ) {
                resources.fault.resolve()
                resources.publisher.getTrackableState(
                    resources.activeTracklableId
                )!!
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
        asyncOp: suspend () -> StateFlow<TrackableState>
    ) {
        withResources { resources ->
            var job: Job? = null
            val completedExpectation = failOnException(actionLabel) {
                job = asyncOp().onEach(receiver::receive).launchIn(resources.scope)
            }

            completedExpectation.await()
            completedExpectation.assertSuccess()
            receiver.outcome.await(DEFAULT_STATE_TRANSITION_TIMEOUT_SECONDS)
            receiver.outcome.assertSuccess()
            runBlocking {
                job?.cancelAndJoin()
            }
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
     * Checks that we have TestResources initialized and executes the test body
     */
    private fun withResources(testBody: (TestResources) -> Unit) {
        if (testResources == null) {
            Assert.fail("Test has not been initialized")
        } else {
            testResources?.let(testBody)
        }
    }
}

/**
 * Common test resources required by all tests above, packaged into a utility
 * class to make setup and teardown consistent and prevent the need for excessive
 * null-checking in every test implementation
 */
class TestResources(
    val activeTracklableId: String,
    val context: Context,
    val scope: CoroutineScope,
    val locationHelper: LocationHelper,
    val fault: FaultSimulation,
    val publisher: Publisher
) {
    companion object {
        /**
         * Initialize common test resources required for all tests
         */
        fun setUp(faultParam: FaultSimulation): TestResources {
            val id = UUID.randomUUID().toString()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val scope =  CoroutineScope(Dispatchers.Unconfined)
            val locationHelper = LocationHelper(id)
            val publisher = createPublisher(context, faultParam.proxy.clientOptions, locationHelper.channelName)

            faultParam.proxy.start()

            return TestResources(
                activeTracklableId = id,
                context = context,
                scope = scope,
                locationHelper = locationHelper,
                fault = faultParam,
                publisher = publisher
            )
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
                    LocationSourceAbly.create(locationChannelName, LOCATION_SOURCE_OPTS),
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

    fun tearDown() {
        scope.cancel()
        val stopExpectation = shutdownPublisher(publisher)
        stopExpectation.assertSuccess()
        locationHelper.close()
        fault.proxy.stop()
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
}

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
 * ClientOptions that will *not* go through a proxy, used to inject location data.
 */
private val LOCATION_SOURCE_OPTS = ClientOptions().apply {
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
    private val opts = LOCATION_SOURCE_OPTS
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