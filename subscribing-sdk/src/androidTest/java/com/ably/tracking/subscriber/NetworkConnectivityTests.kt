package com.ably.tracking.subscriber

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
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
import com.ably.tracking.test.android.common.AttachUnresponsive
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.createNotificationChannel
import com.ably.tracking.test.android.common.DetachUnresponsive
import com.ably.tracking.test.android.common.DisconnectAndSuspend
import com.ably.tracking.test.android.common.DisconnectWithFailedResume
import com.ably.tracking.test.android.common.EnterFailedWithNonfatalNack
import com.ably.tracking.test.android.common.EnterUnresponsive
import com.ably.tracking.test.android.common.FaultSimulation
import com.ably.tracking.test.android.common.FaultType
import com.ably.tracking.test.android.common.NullApplicationLayerFault
import com.ably.tracking.test.android.common.NullTransportFault
import com.ably.tracking.test.android.common.PUBLISHER_CLIENT_ID
import com.ably.tracking.test.android.common.ReenterOnResumeFailed
import com.ably.tracking.test.android.common.SUBSCRIBER_CLIENT_ID
import com.ably.tracking.test.android.common.TcpConnectionRefused
import com.ably.tracking.test.android.common.TcpConnectionUnresponsive
import com.ably.tracking.test.android.common.testLogD
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.UpdateFailedWithNonfatalNack
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.PresenceMessage
import io.ably.lib.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.UUID
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class NetworkConnectivityTests(private val testFault: FaultSimulation) {
    private var testResources: TestResources? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(NullTransportFault(BuildConfig.ABLY_API_KEY)),
            arrayOf(NullApplicationLayerFault(BuildConfig.ABLY_API_KEY)),
            arrayOf(TcpConnectionRefused(BuildConfig.ABLY_API_KEY)),
            arrayOf(TcpConnectionUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(AttachUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(DetachUnresponsive(BuildConfig.ABLY_API_KEY)),
            arrayOf(DisconnectWithFailedResume(BuildConfig.ABLY_API_KEY)),
            arrayOf(EnterFailedWithNonfatalNack(BuildConfig.ABLY_API_KEY)),
            arrayOf(UpdateFailedWithNonfatalNack(BuildConfig.ABLY_API_KEY)),
            arrayOf(DisconnectAndSuspend(BuildConfig.ABLY_API_KEY)),
            arrayOf(ReenterOnResumeFailed(BuildConfig.ABLY_API_KEY)),
            arrayOf(EnterUnresponsive(BuildConfig.ABLY_API_KEY)),
        )
    }

    @Before
    fun setUp() {
        Assume.assumeFalse(testFault.skipSubscriberTest)

        // We cannot use ktor on API Level 21 (Lollipop) because of:
        // https://youtrack.jetbrains.com/issue/KTOR-4751/HttpCache-plugin-uses-ConcurrentMap.computeIfAbsent-method-that-is-available-only-since-Android-API-24
        // We we're only running them if runtime API Level is 24 (N) or above.
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

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
            resources.getSubscriber()
            resources.fault.enable()
            resources.shutdownSubscriber()
        }
    }

    /**
     * Test that Subscriber can handle the given fault occurring whilst tracking.
     *
     * We expect that upon the resolution of the fault, location updates sent in
     * the meantime will be received by the subscriber.
     */
    @Test
    fun faultWhilstTracking() {
        withResources { resources ->
            val subscriber = resources.getSubscriber()

            // Bring a publisher online and send a location update
            var defaultAbly = resources.createAndStartPublishingAblyConnection()
            val locationUpdate = Location(1.0, 2.0, 4000.1, 351.2f, 331.1f, 22.5f, 1234)
            val publisherResolution = Resolution(Accuracy.BALANCED, 1L, 0.0)

            SubscriberMonitor.onlineWithoutFail(
                subscriber = subscriber,
                label = "[no fault] subscriber online",
                trackableId = resources.trackableId,
                locationUpdate = locationUpdate,
                timeout = 10_000L,
                publisherResolution = publisherResolution
            ).waitForStateTransition {
                val locationSent = BooleanExpectation("Location sent successfully on Ably channel")
                defaultAbly.sendEnhancedLocation(
                    resources.trackableId,
                    EnhancedLocationUpdate(
                        locationUpdate,
                        arrayListOf(),
                        arrayListOf(),
                        LocationUpdateType.ACTUAL
                    )
                ) { result ->
                    locationSent.fulfill(result.isSuccess)
                }

                locationSent.await(10)
                locationSent.assertSuccess()
            }

            // Add an active trackable while fault active
            val secondLocationUpdate = Location(2.0, 2.0, 4000.1, 351.2f, 331.1f, 22.5f, 1234)
            val secondPublisherResolution = Resolution(Accuracy.MINIMUM, 100L, 0.0)
            SubscriberMonitor.forActiveFault(
                subscriber = subscriber,
                label = "[fault active] subscriber",
                trackableId = resources.trackableId,
                faultType = resources.fault.type,
                locationUpdate = when (resources.fault.type) {
                    is FaultType.Nonfatal -> secondLocationUpdate
                    else -> locationUpdate
                },
                publisherResolution = when (resources.fault.type) {
                    is FaultType.Nonfatal -> secondPublisherResolution
                    else -> publisherResolution
                }
            ).waitForStateTransition {
                // Start the fault
                resources.fault.enable()

                // Connect up a publisher to do publisher things
                defaultAbly.updatePresenceData(resources.trackableId, PresenceData(ClientTypes.PUBLISHER, secondPublisherResolution, false))

                val locationSent = BooleanExpectation("Location sent successfully on Ably channel")
                defaultAbly.sendEnhancedLocation(
                    resources.trackableId,
                    EnhancedLocationUpdate(
                        secondLocationUpdate,
                        arrayListOf(),
                        arrayListOf(),
                        LocationUpdateType.ACTUAL
                    )
                ) { result ->
                    locationSent.fulfill(result.isSuccess)
                }

                locationSent.await(10)
                locationSent.assertSuccess()
            }.close()

            // Resolve the fault, wait for Trackable to move to expected state
            val thirdLocationUpdate = Location(3.0, 2.0, 4000.1, 351.2f, 331.1f, 22.5f, 1234)
            val thirdPublisherResolution = Resolution(Accuracy.MAXIMUM, 3L, 0.0)
            SubscriberMonitor.forResolvedFault(
                subscriber = subscriber,
                label = "[fault resolved] subscriber",
                trackableId = resources.trackableId,
                faultType = resources.fault.type,
                locationUpdate = thirdLocationUpdate,
                publisherResolution = thirdPublisherResolution
            ).waitForStateTransition {
                defaultAbly.updatePresenceData(resources.trackableId, PresenceData(ClientTypes.PUBLISHER, thirdPublisherResolution, false))

                val locationSent = BooleanExpectation("Location sent successfully on Ably channel")
                defaultAbly.sendEnhancedLocation(
                    resources.trackableId,
                    EnhancedLocationUpdate(
                        thirdLocationUpdate,
                        arrayListOf(),
                        arrayListOf(),
                        LocationUpdateType.ACTUAL
                    )
                ) { result ->
                    locationSent.fulfill(result.isSuccess)
                }

                locationSent.await(10)
                locationSent.assertSuccess()

                // Resolve the problem
                resources.fault.resolve()
            }.close()

            // Restart the fault to simulate the publisher going away whilst we're offline
            SubscriberMonitor.forActiveFault(
                subscriber = subscriber,
                label = "[fault active] publisher shutdown for disconnect test",
                trackableId = resources.trackableId,
                faultType = resources.fault.type,
                locationUpdate = thirdLocationUpdate,
                publisherResolution = thirdPublisherResolution,
                publisherDisconnected = true
            ).waitForStateTransition {
                // Start the fault
                resources.fault.enable()

                // Disconnect the publisher
                resources.shutdownAblyPublishing()
            }.close()

            // Resolve the fault one last time and check that the publisher is offline
            SubscriberMonitor.forResolvedFault(
                subscriber = subscriber,
                label = "[fault resolved] subscriber publisher disconnect test",
                trackableId = resources.trackableId,
                faultType = resources.fault.type,
                locationUpdate = thirdLocationUpdate,
                expectedPublisherPresence = false
            ).waitForStateTransition {
                // Resolve the problem
                resources.fault.resolve()
            }.close()
        }
    }

    /**
     * Test that Subscriber sends resolution preference updates to the publisher
     * after a fault is resolved.
     */
    @OptIn(Experimental::class)
    @Test
    fun faultWhilstUpdatingResolutionPreferenceUpdatesReceivedByPublisherAfterFaultResolution() {
        withResources { resources ->
            // Join the ably channel and listen for presence updates
            val publishingConnection = resources.createAndStartPublishingAblyConnection()

            val initialResolutionPreferenceExpectation = UnitExpectation("Initial resolution preference received")
            var initialResolutionPreference: Resolution? = null
            val receivedResolutionPreferenceExpectation = UnitExpectation("Updated resolution preference received")
            var receivedResolutionPreference: Resolution? = null
            runBlocking {
                publishingConnection.subscribeForPresenceMessages(
                    resources.trackableId,
                    emitCurrentMessages = false,
                    listener = { message ->
                        if (initialResolutionPreference == null) {
                            message.data.resolution?.let {
                                initialResolutionPreference = message.data.resolution
                                initialResolutionPreferenceExpectation.fulfill()
                            }

                            return@subscribeForPresenceMessages
                        }

                        message.data.resolution?.let {
                            receivedResolutionPreference = message.data.resolution
                            receivedResolutionPreferenceExpectation.fulfill()
                        }
                    }
                )
            }

            // Start the subscriber and wait for the initial resolution preference to come through
            val subscriber = resources.getSubscriber()
            initialResolutionPreferenceExpectation.await(10)
            initialResolutionPreferenceExpectation.assertFulfilled()
            Assert.assertEquals(Resolution(Accuracy.BALANCED, 1L, 0.0), initialResolutionPreference)

            // Start the fault and trigger the subscriber sending a new resolution
            resources.fault.enable()
            val newResolutionPreference = Resolution(Accuracy.MAXIMUM, 5L, 2.0)
            runBlocking {
                subscriber.resolutionPreference(newResolutionPreference)
            }

            /**
             * Resolve the fault and check that we then receive the new resolution preference.
             * This has to be a long wait because some of the faults take many minutes to resolve.
             */
            resources.fault.resolve()
            receivedResolutionPreferenceExpectation.await(600)
            receivedResolutionPreferenceExpectation.assertFulfilled()
            Assert.assertEquals(newResolutionPreference, receivedResolutionPreference)
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
                    DefaultAblySdkRealtime(fault.proxy.clientOptions().apply { clientId = SUBSCRIBER_CLIENT_ID })

                override fun wrapChannelStateListener(
                    underlyingListener: AblySdkFactory.UnderlyingChannelStateListener<DefaultAblySdkChannelStateListener>
                ) = DefaultAblySdkChannelStateListener(underlyingListener)
            }
            val connectionConfiguration = ConnectionConfiguration(
                Authentication.basic(
                    SUBSCRIBER_CLIENT_ID,
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
        fun createAndStartPublishingAblyConnection(): DefaultAbly<DefaultAblySdkChannelStateListener> {
            if (ablyPublishing != null) {
                return ablyPublishing!!
            }

            // Configure connection options
            val connectionConfiguration = ConnectionConfiguration(
                Authentication.basic(
                    PUBLISHER_CLIENT_ID,
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
            var receivedFirstOnlineStateChange = false
            val stateChangeExpectation = UnitExpectation("Channel state set to online")
            defaultAbly.subscribeForChannelStateChange(trackableId) { connectionStateChange ->
                if (!receivedFirstOnlineStateChange && connectionStateChange.state == ConnectionState.ONLINE) {
                    receivedFirstOnlineStateChange = true
                    stateChangeExpectation.fulfill()
                }
            }
            stateChangeExpectation.await(10)
            stateChangeExpectation.assertFulfilled()

            ablyPublishing = defaultAbly

            return ablyPublishing!!
        }

        fun tearDown() {
            val stopExpectation = shutdownSubscriber()
            stopExpectation.assertSuccess()
            scope.cancel()
            shutdownAblyPublishing()
            fault.proxy.stop()
        }

        /**
         * If the test has started up a publishing connection to the Ably
         * channel, shut it down.
         */
        fun shutdownAblyPublishing() {
            runBlocking {
                ablyPublishing?.let {
                    testLogD("Shutting down Ably publishing connection")
                    it.close(ablyPublishingPresenceData)
                    testLogD("Ably publishing connection shutdown")
                }
                ablyPublishing = null
            }
        }

        /**
         * Shutdown the subscriber and wait for confirmation, or a timeout.
         * Returns a BooleanExpectation, which can be used to check for successful
         * shutdown of the publisher
         */
        fun shutdownSubscriber(): BooleanExpectation {
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
            subscriber = null
            return stopExpectation
        }
    }
}

/**
 * ClientOptions that will *not* go through a proxy, used to inject location data.
 */
private val CLIENT_OPTS_NO_PROXY = ClientOptions().apply {
    this.clientId = "IntegTests_NoProxy"
    this.key = BuildConfig.ABLY_API_KEY
    this.logHandler = Logging.ablyJavaDebugLogger
}

/**
 * Monitors Subscriber activity so that we can make assertions about any trackable state
 * transitions expected and ensure side-effects occur.
 */
class SubscriberMonitor(
    private val subscriber: Subscriber,
    val label: String,
    val trackableId: String,
    private val expectedState: KClass<out TrackableState>,
    private val failureStates: Set<KClass<out TrackableState>>,
    private val expectedSubscriberPresence: Boolean?,
    private val expectedPublisherPresence: Boolean?,
    private val expectedLocation: Location? = null,
    private val expectedPublisherResolution: Resolution?,
    val timeout: Long,
) {

    private val ably = AblyRealtime(CLIENT_OPTS_NO_PROXY)

    companion object {
        /**
         * Construct [PublisherMonitor] configured to expect appropriate state transitions
         * for the given fault type while it is active. [label] will be used for logging captured transitions.
         */
        fun forActiveFault(
            subscriber: Subscriber,
            label: String,
            trackableId: String,
            faultType: FaultType,
            locationUpdate: Location? = null,
            publisherResolution: Resolution? = null,
            publisherDisconnected: Boolean = false
        ) = SubscriberMonitor(
            subscriber = subscriber,
            label = label,
            trackableId = trackableId,
            expectedState = when {
                faultType is FaultType.Fatal -> TrackableState.Failed::class
                publisherDisconnected && faultType is FaultType.Nonfatal -> TrackableState.Offline::class
                faultType is FaultType.NonfatalWhenResolved -> TrackableState.Offline::class
                else -> TrackableState.Online::class
            },
            failureStates = when (faultType) {
                is FaultType.Fatal -> setOf(TrackableState.Offline::class)
                is FaultType.Nonfatal, is FaultType.NonfatalWhenResolved ->
                    setOf(TrackableState.Failed::class)
            },
            expectedSubscriberPresence = when (faultType) {
                is FaultType.Nonfatal -> true
                is FaultType.NonfatalWhenResolved -> null
                is FaultType.Fatal -> false
            },
            expectedPublisherPresence = when (faultType) {
                is FaultType.Nonfatal -> !publisherDisconnected
                is FaultType.NonfatalWhenResolved -> false
                is FaultType.Fatal -> false
            } ,
            expectedLocation = locationUpdate,
            timeout = when (faultType) {
                is FaultType.Fatal -> faultType.failedWithinMillis
                is FaultType.Nonfatal -> faultType.resolvedWithinMillis
                is FaultType.NonfatalWhenResolved -> faultType.offlineWithinMillis
            },
            expectedPublisherResolution = publisherResolution
        )

        /**
         * Construct a [PublisherMonitor] configured to expect appropriate transitions for
         * the given fault type after it has been resolved. [label] is used for logging.
         */
        fun forResolvedFault(
            subscriber: Subscriber,
            label: String,
            trackableId: String,
            faultType: FaultType,
            locationUpdate: Location? = null,
            publisherResolution: Resolution? = null,
            expectedPublisherPresence: Boolean = true
        ) = SubscriberMonitor(
            subscriber = subscriber,
            label = label,
            trackableId = trackableId,
            expectedState = when {
                !expectedPublisherPresence -> TrackableState.Offline::class
                faultType is FaultType.Fatal -> TrackableState.Failed::class
                faultType is FaultType.Nonfatal || faultType is FaultType.NonfatalWhenResolved ->
                    TrackableState.Online::class
                else -> TrackableState.Offline::class
            },
            failureStates = when (faultType) {
                is FaultType.Fatal -> setOf(
                    TrackableState.Offline::class,
                    TrackableState.Online::class
                )
                is FaultType.Nonfatal, is FaultType.NonfatalWhenResolved ->
                    setOf(TrackableState.Failed::class)
            },
            expectedSubscriberPresence = when (faultType) {
                is FaultType.Fatal -> false
                else -> true
            },
            expectedPublisherPresence = expectedPublisherPresence,
            expectedLocation = locationUpdate,
            timeout = when (faultType) {
                is FaultType.Fatal -> faultType.failedWithinMillis
                is FaultType.Nonfatal -> faultType.resolvedWithinMillis
                is FaultType.NonfatalWhenResolved -> faultType.offlineWithinMillis
            },
            expectedPublisherResolution = publisherResolution
        )

        /**
         * Construct a [PublisherMonitor] configured to expect a Trackable to come
         * online within a given timeout, and fail if the Failed state is seen at any point.
         */
        fun onlineWithoutFail(
            subscriber: Subscriber,
            label: String,
            trackableId: String,
            timeout: Long,
            locationUpdate: Location? = null,
            publisherResolution: Resolution? = null,
        ) = SubscriberMonitor(
            subscriber,
            label = label,
            trackableId = trackableId,
            expectedState = TrackableState.Online::class,
            failureStates = setOf(TrackableState.Failed::class),
            expectedSubscriberPresence = true,
            expectedPublisherPresence = true,
            expectedLocation = locationUpdate,
            timeout = timeout,
            expectedPublisherResolution = publisherResolution
        )
    }

    /**
     * Performs the given async (suspending) operation in a runBlocking, attaching the
     * returned StateFlow<TrackableState> to the given receiver, then waits for expectations
     * to be delivered (or not) before cleaning up.
     */
    fun waitForStateTransition(
        asyncOp: suspend () -> Unit
    ): SubscriberMonitor {
        runBlocking {
            try {
                withTimeout(timeout) {
                    asyncOp()
                    testLogD("$label - async op success")

                    assertStateTransition()
                    assertSubscriberPresence()
                    assertPublisherPresence()
                    assertLocationUpdated()
                    assertPublisherResolution()
                }
            } catch (timeoutCancellationException: TimeoutCancellationException) {
                testLogD("$label - timed out")
                throw AssertionError("$label timed out.", timeoutCancellationException)
            } catch (exception: Exception) {
                testLogD("$label - failed - $exception")
                throw AssertionError("$label did not result in success.", exception)
            }
        }

        return this
    }

    private fun assertPublisherResolution()
    {
        if (expectedPublisherResolution == null) {
            testLogD("SubscriberMonitor: $label - (SKIP) expectedPublisherResolution = null")
            return
        }

        testLogD("SubscriberMonitor: $label - (WAITING) expectedPublisherResolution = $expectedPublisherResolution")
        val lastPublisherResolution = listenForExpectedPublisherResolution()
        if (lastPublisherResolution != expectedPublisherResolution) {
            testLogD("SubscriberMonitor: $label - (FAIL) lastPublisherResolution = $lastPublisherResolution")
            throw AssertionError(
                "Expected resolution update $expectedPublisherResolution but last was $lastPublisherResolution"
            )
        } else {
            testLogD("SubscriberMonitor: $label - (PASS) lastPublisherResolution = $lastPublisherResolution")
        }
    }

    /**
     * Uses the subscribers publisher presence state flow to listen for the expected
     * publisher resolution change.
     *
     * This can happen at any time after the initial trackable state transition,
     * and so we cannot rely on the first state we collect being the "newest" one.
     */
    private fun listenForExpectedPublisherResolution(): Resolution
    {
        val lastResolution = runBlocking {
            subscriber.resolutions.first{resolution -> resolution == expectedPublisherResolution}
        }

        testLogD("lastPublisherResolution: $lastResolution")
        return lastResolution;
    }

    /**
     * Throw an assertion error of the expected [TrackableState] transition hasn't happened.
     */
    private suspend fun assertStateTransition() {
        testLogD("$label Awaiting state transition to $expectedState")
        val result = subscriber.trackableStates.mapNotNull { receive(it) }.first()
        if (!result) {
            throw AssertionError("Expectation '$label' publisher presence did not result in success.")
        }
    }

    /**
     * Maps received [TrackableState] to a success/fail/ignore outcome for this test.
     */
    private fun receive(state: TrackableState): Boolean? =
        when {
            failureStates.contains(state::class) -> {
                testLogD("SubscriberMonitor (FAIL): $label - $state")
                false
            }
            expectedState == state::class -> {
                testLogD("SubscriberMonitor (SUCCESS): $label - $state")
                true
            }
            else -> {
                testLogD("SubscriberMonitor (IGNORED): $label - $state")
                null
            }
        }

    /**
     * Assert that we eventually receive the expected publisher presence.
     *
     * This can happen at any time after the initial trackable state transition,
     * and so we cannot rely on the first state we collect being the "new" one.
     */
    @OptIn(Experimental::class)
    private fun assertPublisherPresence() = runBlocking {
        testLogD("SubscriberMonitor (WAITING): $label - publisher presence -> $expectedPublisherPresence")
        val presence = subscriber.publisherPresence.first{presence -> presence == expectedPublisherPresence}
        testLogD("SubscriberMonitor (PASS): $label - publisher presence was $presence")
    }

    /**
     * Throw an assertion error if the subscriber's presence does not meet expectations for this test.
     */
    private fun assertSubscriberPresence() {
        if (expectedSubscriberPresence == null) {
            // not checking for publisher presence in this test
            testLogD("SubscriberMonitor: $label - (SKIP) expectedSubscriberPresence = null")
            return
        }

        val publisherPresent = subscriberIsPresent()
        if (publisherPresent != expectedSubscriberPresence) {
            testLogD("SubscriberMonitor: $label - (FAIL) subscriberPresent = $publisherPresent")
            throw AssertionError(
                "Expected subscriberPresence: $expectedSubscriberPresence but got $publisherPresent"
            )
        } else {
            testLogD("SubscriberMonitor: $label - (PASS) subscriberPresent = $publisherPresent")
        }
    }

    /**
     * Perform a request to the Ably API to get a snapshot of the current presence for the channel,
     * and check to see if the Subscriber's clientId is present in that snapshot.
     */
    private fun subscriberIsPresent() =
        ably.channels
            .get("tracking:${trackableId}")
            ?.presence
            ?.get(true)
            ?.find {
                it.clientId == SUBSCRIBER_CLIENT_ID &&
                    it.action == PresenceMessage.Action.present
            } != null

    /**
     * Throw an assertion error if expectations about published location updates have not
     * been meet in this test.
     */
    private fun assertLocationUpdated() {
        if (expectedLocation == null) {
            // no expected location set - skip assertion
            testLogD("SubscriberMonitor: $label - (SKIP) expectedLocationUpdate = null")
            return
        }

        testLogD("SubscriberMonitor: $label - (WAITING) expectedLocationUpdate = $expectedLocation")
        val lastPublishedLocation = listenForExpectedLocationUpdate()
        if (lastPublishedLocation != expectedLocation) {
            testLogD("SubscriberMonitor: $label - (FAIL) lastPublishedLocation = $lastPublishedLocation")
            throw AssertionError(
                "Expected location update $expectedLocation but last was $lastPublishedLocation"
            )
        } else {
            testLogD("SubscriberMonitor: $label - (PASS) lastPublishedLocation = $lastPublishedLocation")
        }
    }

    /**
     * Use the subscriber location flow to listen for a location update matching the one we're expecting.
     *
     * These location updates may arrive at any time after the trackable transitions to online, so we therefore
     * cannot rely on the first thing we find being the "newest" state and therefore must wait for a bit.
     */
    private fun listenForExpectedLocationUpdate(): Location {
        val lastLocation: Location = runBlocking {
            subscriber.locations.first{locationUpdate -> locationUpdate.location == expectedLocation}.location
        }

        testLogD("lastLocation: $lastLocation")
        return lastLocation;
    }

    /**
     * Close any open resources used by this monitor.
     */
    fun close() {
        ably.close()
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
