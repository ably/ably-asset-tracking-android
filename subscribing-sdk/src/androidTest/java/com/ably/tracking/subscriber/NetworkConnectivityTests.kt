package com.ably.tracking.subscriber

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.common.AblySdkFactory
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.DefaultAblySdkFactory
import com.ably.tracking.common.DefaultAblySdkRealtime
import com.ably.tracking.common.DefaultAblySdkChannelStateListener
import com.ably.tracking.common.PresenceData
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.test.android.common.*
import io.ably.lib.types.ClientOptions
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
            /*arrayOf(TcpConnectionUnresponsive(BuildConfig.ABLY_API_KEY)),
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
     * starts the subscriber.
     *
     * We expect the subscriber to not throw an exception.
     */
    @Test
    fun faultBeforeStartingSubscriber() {
        withResources { resources ->
            resources.fault.enable()
            resources.getSubscriber()
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring after a user
     * starts the subscriber and then proceeds to stop it.
     *
     * We expect the subscriber to stop cleanly, with no exceptions.
     */
    @Test
    fun faultAfterStartingSubscriber() {
        withResources { resources ->
            val subscriber = resources.getSubscriber()
            resources.fault.enable()
            runBlocking {
                subscriber.stop()
            }
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring whilst tracking.
     *
     * We expect that upon the resolution of the fault, location updates sent in
     * the meantime will be received by the subscriber.
     */
    @Test
    fun faultWhilstTrackingLocationUpdatesArriveAfterResolution() {
        withResources { resources ->
            val subscriber = resources.getSubscriber()
            resources.fault.enable()

            // Send an enhanced location update on the channel
            val defaultAbly = resources.createAndStartPublishingAblyConnection()
            val locationSent = BooleanExpectation("Location sent successfully on Ably channel")
            val location = Location(
                1.0,
                2.0,
                3000.9,
                1f,
                341f,
                1.5f,
                1234,
            )
            defaultAbly.sendEnhancedLocation(
                resources.trackableId,
                EnhancedLocationUpdate(
                    location,
                    arrayListOf(),
                    arrayListOf(),
                    LocationUpdateType.ACTUAL
                )
            ) { result ->
                locationSent.fulfill(result.isSuccess)
            }

            locationSent.await(10)
            locationSent.assertSuccess()

            // Resolve the fault and check that we then receive the position update
            val locationReceived = UnitExpectation("Position received by subscriber")
            var receivedLocationUpdate: LocationUpdate? = null

            subscriber.locations
                .onEach { locationUpdate ->
                    receivedLocationUpdate = locationUpdate
                    locationReceived.fulfill()
                }
                .launchIn(resources.scope)

            // This has to be a long wait because some of the faults take many minutes to resolve
            resources.fault.resolve()
            locationReceived.await(600)
            locationReceived.assertFulfilled()

            Assert.assertEquals(location, receivedLocationUpdate!!.location)
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring whilst tracking.
     *
     * Check that after resolution, trackable states (via channel presence)
     * are updated.
     */
    @Test
    fun faultWhilstTrackingTrackableStatesArriveAfterResolution() {
        withResources { resources ->
            val subscriber = resources.getSubscriber()
            resources.fault.enable()

            // Begin listening for trackable state changes - the initial state is offline, so we have to ignore the first one
            val initialTrackableStateReceived = UnitExpectation("Initial trackable offline state received")
            var initialTrackableOfflineStateReceived = false;
            val trackableOnlineReceived = UnitExpectation("Trackable online state received")
            val trackableOfflineReceived = UnitExpectation("Trackable offline state received")

            subscriber.trackableStates
                .onEach { state ->
                    if (state == TrackableState.Online) {
                        trackableOnlineReceived.fulfill()
                    }

                    if (state == TrackableState.Offline()) {
                        if (!initialTrackableOfflineStateReceived) {
                            initialTrackableOfflineStateReceived = true;
                            initialTrackableStateReceived.fulfill()
                            return@onEach
                        }

                        trackableOfflineReceived.fulfill()
                    }
                }
                .launchIn(resources.scope)

            initialTrackableStateReceived.await(10)
            initialTrackableStateReceived.assertFulfilled()

            // Join the ably channel as a publisher to trigger a trackable online state
            resources.createAndStartPublishingAblyConnection()

            /**
             * Resolve the fault and check that we then receive the trackable online state.
             * This has to be a long wait because some of the faults take many minutes to resolve.
             */
            resources.fault.resolve()
            trackableOnlineReceived.await(600)
            trackableOnlineReceived.assertFulfilled()

            // Re-enable the fault and shutdown the publisher to trigger a trackable offline state
            resources.fault.enable()
            resources.shutdownAblyPublishing()

            // Resolve the fault again and make sure we receive the trackable offline state
            resources.fault.resolve()
            trackableOfflineReceived.await(600)
            trackableOfflineReceived.assertFulfilled()
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring whilst tracking,
     *
     * Check that after resolution, changes to the channels publisher presence
     * are received by the subscriber.
     */
    @OptIn(Experimental::class)
    @Test
    fun faultWhilstTrackingPublisherPresenceUpdatesReceivedAfterResolution() {
        withResources { resources ->
            val subscriber = resources.getSubscriber()
            resources.fault.enable()

            // Begin listening for publisher state changes - the initial state if offline, so we have to ignore the first one
            val initialPublisherOfflineStateReceived = UnitExpectation("Initial trackable offline state received")
            var initialNoPresenceReceived = false;
            val publisherOnlineReceived = UnitExpectation("Publisher online state received")
            val publisherOfflineReceived = UnitExpectation("Publisher offline state received")

            subscriber.publisherPresence
                .onEach { presence ->
                    if (presence) {
                        publisherOnlineReceived.fulfill()
                    } else if (!initialNoPresenceReceived) {
                        initialNoPresenceReceived = true
                        initialPublisherOfflineStateReceived.fulfill()
                    } else {
                        publisherOfflineReceived.fulfill()
                    }
                }
                .launchIn(resources.scope)

            initialPublisherOfflineStateReceived.await(10)
            initialPublisherOfflineStateReceived.assertFulfilled()

            // Join the ably channel to trigger a publisher online state change
            resources.createAndStartPublishingAblyConnection()

            /**
             * Resolve the fault and check that we then receive the publisher state updates.
             * This has to be a long wait because some of the faults take many minutes to resolve.
             */
            resources.fault.resolve()
            publisherOnlineReceived.await(600)
            publisherOnlineReceived.assertFulfilled()

            // Restart and disconnect the publisher
            resources.fault.enable()
            resources.shutdownAblyPublishing()

            // Re-resolve the fault and check we see the publisher go offline again
            resources.fault.resolve()
            publisherOfflineReceived.await(600)
            publisherOfflineReceived.assertFulfilled()
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
        val trackableId: String
    ) {

        private var subscriber: Subscriber? = null
        private var ablyPublishing: DefaultAbly<DefaultAblySdkChannelStateListener>? = null
        private val ablyPublishingPresenceData = PresenceData(ClientTypes.PUBLISHER, Resolution(Accuracy.BALANCED, 1L, 0.0))

        companion object {

            /**
             * Initialize common test resources required for all tests
             */
            fun setUp(faultParam: FaultSimulation): TestResources {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val scope = CoroutineScope(Dispatchers.Unconfined)
                val trackableId = UUID.randomUUID().toString()

                faultParam.proxy.start()

                return TestResources(
                    context = context,
                    fault = faultParam,
                    scope = scope,
                    trackableId = trackableId
                )
            }
        }

        /**
         * Injects a pre-configured AblyRealtime instance to the Subscriber by constructing it
         * and all dependencies by hand, side-stepping the builders, which block this.
         *
         * This is a function rather than passed into the constructor as one of the tests requires
         * the subscriber not to be started prior to test commencement.
         */
        fun getSubscriber(): Subscriber {

            if (subscriber != null) {
                return subscriber!!
            }

            val ablySdkFactory = object : AblySdkFactory<DefaultAblySdkChannelStateListener> {
                override fun createRealtime(clientOptions: ClientOptions) =
                    DefaultAblySdkRealtime(fault.proxy.clientOptions())

                override fun wrapChannelStateListener(
                    underlyingListener: AblySdkFactory.UnderlyingChannelStateListener<DefaultAblySdkChannelStateListener>
                ) = DefaultAblySdkChannelStateListener(underlyingListener)
            }
            val connectionConfiguration = ConnectionConfiguration(
                Authentication.basic(
                    fault.proxy.clientOptions().clientId,
                    fault.proxy.clientOptions().key
                )
            )

            runBlocking {
                subscriber = DefaultSubscriber(
                    DefaultAbly(
                        ablySdkFactory,
                        connectionConfiguration,
                        Logging.aatDebugLogger
                    ),
                    Resolution(Accuracy.BALANCED, 1L, 0.0),
                    trackableId,
                    Logging.aatDebugLogger
                ).apply { start() }
            }

            return subscriber!!
        }

        /**
         * Creates and starts a connection to an Ably channel for the purpose of publishing location
         * updates and emulating trackable state change events.
         */
        fun createAndStartPublishingAblyConnection(): DefaultAbly<DefaultAblySdkChannelStateListener>
        {
            if (ablyPublishing != null) {
                return ablyPublishing!!
            }

            // Configure connection options
            val connectionConfiguration = ConnectionConfiguration(
                Authentication.basic(
                    "SubscriberNetworkConnectivityTestsDummyPublisher",
                    fault.proxy.clientOptions().key
                )
            )

            // Connect to ably
            val defaultAbly = DefaultAbly(
                DefaultAblySdkFactory(),
                connectionConfiguration,
                Logging.aatDebugLogger
            )

            runBlocking {
                Assert.assertTrue(defaultAbly.startConnection().isSuccess)
            }

            val connectedToAbly = BooleanExpectation("Successfully connected to Ably")
            defaultAbly.connect(
                trackableId,
                ablyPublishingPresenceData,
                useRewind = true,
                willPublish = true,
            ) { result ->
                connectedToAbly.fulfill(result.isSuccess)
            }
            connectedToAbly.await(10)
            connectedToAbly.assertSuccess()

            // Wait for channel to come online
            val stateChangeExpectation = UnitExpectation("Channel state set to online")
            defaultAbly.subscribeForChannelStateChange(trackableId) { connectionStateChange ->
                if (connectionStateChange.state == ConnectionState.ONLINE) {
                    stateChangeExpectation.fulfill()
                }
            }
            stateChangeExpectation.await(10)
            stateChangeExpectation.assertFulfilled()

            ablyPublishing = defaultAbly;

            return ablyPublishing!!
        }

        fun tearDown() {
            val stopExpectation = shutdownSubscriber()
            stopExpectation.assertSuccess()
            scope.cancel()
            fault.proxy.stop()
            shutdownAblyPublishing()
        }

        /**
         * If the test has started up a publishing connection to the Ably
         * channel, shut it down.
         */
        fun shutdownAblyPublishing()
        {
            runBlocking {
                ablyPublishing?.close(ablyPublishingPresenceData)
                ablyPublishing = null
            }
        }

        /**
         * Shutdown the subscriber and wait for confirmation, or a timeout.
         * Returns a BooleanExpectation, which can be used to check for successful
         * shutdown of the publisher
         */
        private fun shutdownSubscriber(): BooleanExpectation {
            val stopExpectation = BooleanExpectation("stop response")
            runBlocking {
                try {
                    subscriber?.stop()
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
    }
}
