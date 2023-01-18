package com.ably.tracking.subscriber

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.*
import com.ably.tracking.common.*
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.test.android.common.*
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Param
import io.ably.lib.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

@RunWith(Parameterized::class)
class NetworkConnectivityTests(private val testFault: FaultSimulation) {
    private var testResources: TestResources? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(NullTransportFault(BuildConfig.ABLY_API_KEY)),
            arrayOf(NullApplicationLayerFault(BuildConfig.ABLY_API_KEY)),
/*            arrayOf(TcpConnectionRefused(BuildConfig.ABLY_API_KEY)),
            arrayOf(TcpConnectionUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(AttachUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(DetachUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(DisconnectWithFailedResume(BuildConfig.ABLY_API_KEY)),
            arrayOf(EnterFailedWithNonfatalNack(BuildConfig.ABLY_API_KEY)),
            arrayOf(UpdateFailedWithNonfatalNack(BuildConfig.ABLY_API_KEY)),
            arrayOf(DisconnectAndSuspend(BuildConfig.ABLY_API_KEY)),
            arrayOf(ReenterOnResumeFailed(BuildConfig.ABLY_API_KEY)),
            arrayOf(EnterUnresponsive(BuildConfig.ABLY_API_KEY)),*/
        )
    }

    @Before
    fun setUp() {
        Assume.assumeFalse(testFault.skipTest)

        testResources = TestResources.setUp(testFault)
        createNotificationChannel(testResources?.context!!)
    }

    @After
    fun tearDown() {
        testResources?.tearDown()
    }

    /**
     * Test that Subscriber can handle the given fault occurring before a user
     * starts the subscriber, and does not throw an exception.
     *
     * TODO: Add more detail here about expectations
     */
    @Test
    fun faultBeforeStartingSubscriber()
    {
        withResources { resources ->
            resources.fault.enable()
            resources.subscriber()
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring after a user
     * starts the subscriber, and does not throw an exception.
     *
     * TODO: Add more detail here about expectations
     */
    @Test
    fun faultAfterStartingSubscriber()
    {
        withResources { resources ->
            resources.subscriber()
            resources.fault.enable()
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring whilst tracking,
     * check that after fault resolution, a location update is received.
     *
     * TODO: Add more detail here about expectations
     */
    @Test
    fun faultWhilstTrackingLocationUpdatesArriveAfterResolution()
    {
        withResources { resources ->
            val subscriber = resources.subscriber()
            resources.fault.enable()

            // Send an enhanced location update on the channel
            val connectionConfiguration = ConnectionConfiguration(
                Authentication.basic(
                    "SubscriberNetworkConnectivityTestsDummyPublisher",
                    resources.fault.proxy.clientOptions().key
                )
            )
            val defaultAbly = DefaultAbly(DefaultAblySdkFactory(), connectionConfiguration, Logging.aatDebugLogger)
            val location = Location(
                1.0,
                2.0,
                3000.9,
                1f,
                341f,
                1.5f,
                1234,
            )

            val presenceData = PresenceData(ClientTypes.PUBLISHER, Resolution(Accuracy.BALANCED, 1L, 0.0))

            runBlocking {
                Assert.assertTrue(defaultAbly.startConnection().isSuccess)
            }

            val connectedToAbly = BooleanExpectation("Successfully connected to Ably")
            defaultAbly.connect(
                resources.trackableId,
                presenceData,
                useRewind = true,
                willSubscribe = true,
            ) {result ->
                connectedToAbly.fulfill(result.isSuccess)
            }
            connectedToAbly.await(10)
            connectedToAbly.assertSuccess()

            // Channel state
            val stateChangeExpectation = UnitExpectation("Test")
            defaultAbly.subscribeForChannelStateChange(resources.trackableId) { connectionStateChange ->
                if (connectionStateChange.state == ConnectionState.ONLINE) {
                    stateChangeExpectation.fulfill()
                } else {
                    connectionStateChange.state == ConnectionState.ONLINE
                }
            }
            stateChangeExpectation.await(10)
            stateChangeExpectation.assertFulfilled()

            // Sub to presence
            var success = false;
            defaultAbly.subscribeForPresenceMessages(resources.trackableId, {}, {
                success = it.isSuccess
            })
            success != false


            val locationSent = BooleanExpectation("Location sent successfully on Ably channel")
            defaultAbly.sendEnhancedLocation(
                resources.trackableId,
                EnhancedLocationUpdate(location, arrayListOf(), arrayListOf(), LocationUpdateType.ACTUAL)
            ) { result ->
                locationSent.fulfill(result.isSuccess)
            }

            locationSent.await(10)
            locationSent.assertSuccess()

           // Cancel the fault and check that we've received it correctly
            resources.fault.resolve()

            val locationReceived = UnitExpectation("Position received by subscriber")
            var receivedLocationUpdate: LocationUpdate? = null

            subscriber.locations
                .onEach { locationUpdate ->
                    receivedLocationUpdate = locationUpdate
                    locationReceived.fulfill()
                }
                .launchIn(resources.scope)
            locationReceived.await(10)
            locationReceived.assertFulfilled()

            Assert.assertEquals(location, receivedLocationUpdate!!.location)
        }
    }

    /**
     * Checks that we have TestResources initialized and executes the test body
     */
    private fun withResources(testBody: (TestResources) -> Unit) {
        if (testResources == null) {
            Assert.fail("Test has not been initialized")
        } else {
            testResources!!.let(testBody)
        }
    }

    /**
     * Common test resources required by all tests above, packaged into a utility
     * class to make setup and teardown consistent and prevent the need for excessive
     * null-checking in every test implementation
     */
    class TestResources(
        val context: Context,
        val fault: FaultSimulation,
        val scope: CoroutineScope,
        val trackableId: String,
        val subscriber: () -> Subscriber
    ) {
        companion object {

            /**
             * Initialize common test resources required for all tests
             */
            fun setUp(faultParam: FaultSimulation): TestResources {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val scope = CoroutineScope(Dispatchers.Unconfined)
                val trackableId = UUID.randomUUID().toString()
                val subscriber = createSubscriber(faultParam.proxy.clientOptions(), trackableId)

                faultParam.proxy.start()

                return TestResources(
                    context = context,
                    fault = faultParam,
                    scope = scope,
                    trackableId = trackableId,
                    subscriber = subscriber
                )
            }

            /**
             * Injects a pre-configured AblyRealtime instance to the Subscriber by constructing it
             * and all dependencies by hand, side-stepping the builders, which block this.
             *
             * Returns this as a lambda function so that we can run tests that add the fault before the subscriber
             * is started.
             */
            private fun createSubscriber(
                proxyClientOptions: ClientOptions,
                //TODO: Change this to trackable name later?
                trackableId: String
            ): () -> Subscriber {
                val ablySdkFactory = object : AblySdkFactory<DefaultAblySdkChannelStateListener> {
                    override fun createRealtime(clientOptions: ClientOptions) = DefaultAblySdkRealtime(proxyClientOptions)

                    override fun wrapChannelStateListener(
                        underlyingListener: AblySdkFactory.UnderlyingChannelStateListener<DefaultAblySdkChannelStateListener>
                    ) = DefaultAblySdkChannelStateListener(underlyingListener)
                }
                val connectionConfiguration = ConnectionConfiguration(
                    Authentication.basic(
                        proxyClientOptions.clientId,
                        proxyClientOptions.key
                    )
                )

                var subscriber: Subscriber? = null

                return {
                    // Make sure the subscriber is fully started before we allow things to continue
                    runBlocking {
                        if (subscriber == null) {
                            subscriber = DefaultSubscriber(
                                DefaultAbly(ablySdkFactory, connectionConfiguration, Logging.aatDebugLogger),
                                Resolution(Accuracy.BALANCED, 1L, 0.0),
                                trackableId,
                                Logging.aatDebugLogger
                            ).apply { start() }
                        }

                        subscriber!!
                    }
                }
            }
        }

        fun tearDown() {
            val stopExpectation = shutdownSubscriber(subscriber())
            stopExpectation.assertSuccess()
            scope.cancel()
            fault.proxy.stop()
        }

        /**
         * Shutdown the given subscriber and wait for confirmation, or a timeout.
         * Returns a BooleanExpectation, which can be used to check for successful
         * shutdown of the publisher
         */
        private fun shutdownSubscriber(subscriber: Subscriber): BooleanExpectation {
            val stopExpectation = BooleanExpectation("stop response")
            runBlocking {
                try {
                    subscriber.stop()
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
}
