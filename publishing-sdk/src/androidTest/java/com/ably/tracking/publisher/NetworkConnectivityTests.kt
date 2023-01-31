package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.Location
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.AblySdkFactory
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.DefaultAblySdkChannelStateListener
import com.ably.tracking.common.DefaultAblySdkRealtime
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.message.EnhancedLocationUpdateMessage
import com.ably.tracking.common.message.GeoJsonTypes
import com.ably.tracking.common.message.LocationGeometry
import com.ably.tracking.common.message.LocationMessage
import com.ably.tracking.common.message.LocationProperties
import com.ably.tracking.common.message.getLocationMessages
import com.ably.tracking.common.message.synopsis
import com.ably.tracking.common.message.toTracking
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.test.android.common.AttachUnresponsive
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.DetachUnresponsive
import com.ably.tracking.test.android.common.DisconnectAndSuspend
import com.ably.tracking.test.android.common.DisconnectWithFailedResume
import com.ably.tracking.test.android.common.EnterFailedWithNonfatalNack
import com.ably.tracking.test.android.common.EnterUnresponsive
import com.ably.tracking.test.android.common.Fault
import com.ably.tracking.test.android.common.FaultSimulation
import com.ably.tracking.test.android.common.FaultType
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.ably.tracking.test.android.common.NullApplicationLayerFault
import com.ably.tracking.test.android.common.NullTransportFault
import com.ably.tracking.test.android.common.PUBLISHER_CLIENT_ID
import com.ably.tracking.test.android.common.ReenterOnResumeFailed
import com.ably.tracking.test.android.common.TcpConnectionRefused
import com.ably.tracking.test.android.common.TcpConnectionUnresponsive
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.UpdateFailedWithNonfatalNack
import com.ably.tracking.test.android.common.createNotificationChannel
import com.ably.tracking.test.android.common.testLogD
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.types.PresenceMessage
import io.ably.lib.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN

@RunWith(Parameterized::class)
class NetworkConnectivityTests(private val testFault: Fault) {

    private var testResources: TestResources? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(NullTransportFault.fault),
            arrayOf(NullApplicationLayerFault.fault),
            arrayOf(TcpConnectionRefused.fault),
            arrayOf(TcpConnectionUnresponsive.fault),
            arrayOf(AttachUnresponsive.fault),
            arrayOf(DetachUnresponsive.fault),
            arrayOf(DisconnectWithFailedResume.fault),
            arrayOf(EnterFailedWithNonfatalNack.fault),
            arrayOf(UpdateFailedWithNonfatalNack.fault),
            arrayOf(DisconnectAndSuspend.fault),
            arrayOf(ReenterOnResumeFailed.fault),
            arrayOf(EnterUnresponsive.fault),
        )
    }

    @Before
    fun setUp() {
        val simulation = testFault.simulate(BuildConfig.ABLY_API_KEY)
        Assume.assumeFalse(simulation.skipTest)

        // We cannot use ktor on API Level 21 (Lollipop) because of:
        // https://youtrack.jetbrains.com/issue/KTOR-4751/HttpCache-plugin-uses-ConcurrentMap.computeIfAbsent-method-that-is-available-only-since-Android-API-24
        // We we're only running them if runtime API Level is 24 (N) or above.
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        testResources = TestResources.setUp(simulation)
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
        val primaryTrackable = Trackable(UUID.randomUUID().toString())
        val secondaryTrackable = Trackable(UUID.randomUUID().toString())
        testLogD("PRIMARY: $primaryTrackable")

        withResources {
            fault.enable()

            // Add an active trackable while fault active
            val locationUpdate = locationHelper.locationUpdate(80.0, 100.0)
            PublisherMonitor.forActiveFault(
                label = "[fault active] publisher.track()",
                trackable = primaryTrackable,
                faultType = fault.type,
                locationUpdate = locationUpdate
            ).waitForStateTransition {
                publisher.track(primaryTrackable).also {
                    locationHelper.sendUpdate(locationUpdate)
                }
            }.close()

            // Add a secondary (not active) trackable too
            PublisherMonitor.forActiveFault(
                label = "[fault active] publisher.add()",
                trackable = secondaryTrackable,
                faultType = fault.type,
            ).waitForStateTransition {
                publisher.add(secondaryTrackable).also {
                    // apparently another location update is needed for this to go online
                    locationHelper.sendUpdate(
                        locationHelper.locationUpdate(81.0, 101.0)
                    )
                }
            }.close()

            // Remove that second trackable while fault still active
            runBlocking {
                Assert.assertTrue(publisher.remove(secondaryTrackable))
            }

            // / Resolve the fault and ensure active trackable reaches intended state
            PublisherMonitor.forResolvedFault(
                label = "[fault resolved] publisher.getTrackableState()",
                trackable = primaryTrackable,
                faultType = fault.type
            ).waitForStateTransition {
                fault.resolve()
                publisher.getTrackableState(primaryTrackable.id)!!
            }.close()

            Assert.assertNull(
                publisher.getTrackableState(secondaryTrackable.id)
            )
        }
    }

    /**
     * Tests that tracking of a Trackable recovers if a connectivity fault
     * occurs after a Trackable has been added and already reached the
     * Online state. Also adds a secondary trackable (non-active) before the fault
     * occurs, ensures this reaches the online state too, then attempts to remove it
     * during the fault.
     */
    @Test
    fun faultDuringTracking() {
        val primaryTrackable = Trackable(UUID.randomUUID().toString())
        val secondaryTrackable = Trackable(UUID.randomUUID().toString())

        withResources {
            // Add active trackable, wait for it to reach Online state
            val locationUpdate = locationHelper.locationUpdate(82.0, 102.0)
            PublisherMonitor.onlineWithoutFail(
                label = "[no fault] publisher.track()",
                trackable = primaryTrackable,
                locationUpdate = locationUpdate,
                timeout = 10_000L
            ).waitForStateTransition {
                publisher.track(primaryTrackable).also {
                    locationHelper.sendUpdate(locationUpdate)
                }
            }.close()

            // Add another (non-active) trackable and wait for it to be online
            PublisherMonitor.onlineWithoutFail(
                label = "[no fault] publisher.add()",
                trackable = secondaryTrackable,
                timeout = 10_000L
            ).waitForStateTransition {
                publisher.add(secondaryTrackable).also {
                    // apparently another location update is needed for this to go online
                    locationHelper.sendUpdate(
                        locationHelper.locationUpdate(83.0, 103.0)
                    )
                }
            }.close()

            // Enable the fault, wait for Trackable to move to expected state
            PublisherMonitor.forActiveFault(
                label = "[fault active] publisher.getTrackableState(primary)",
                trackable = primaryTrackable,
                faultType = fault.type
            ).waitForStateTransition {
                fault.enable()
                publisher.getTrackableState(primaryTrackable.id)!!
            }.close()

            // Ensure secondary trackable is also now in expected fault state
            PublisherMonitor.forActiveFault(
                label = "[fault active] publisher.getTrackableState(secondary)",
                trackable = secondaryTrackable,
                faultType = fault.type
            ).waitForStateTransition {
                publisher.getTrackableState(secondaryTrackable.id)!!
            }.close()

            // Remove the secondary trackable while fault is active
            runBlocking {
                Assert.assertTrue(publisher.remove(secondaryTrackable))
            }

            // Resolve the fault, wait for Trackable to move to expected state
            PublisherMonitor.forResolvedFault(
                label = "[fault resolved] publisher.getTrackableState(primary)",
                trackable = primaryTrackable,
                faultType = fault.type
            ).waitForStateTransition {
                fault.resolve()
                publisher.getTrackableState(primaryTrackable.id)!!
            }.close()

            // Ensure that the secondary Trackable is gone
            Assert.assertNull(
                publisher.getTrackableState(secondaryTrackable.id)
            )
        }
    }

    /**
     * Try stopping the publisher during a fault, and then creating a new instance.
     * Resolve the fault and ensure that the new instance is operating normally.
     */
    @Test
    fun faultDuringPublisherRestart() {
        withResources {
            // Enable the fault and restart the publisher
            fault.enable()
            runBlocking {
                publisher.stop()
            }

            val newPublisher = TestResources.createPublisher(
                context = context,
                proxyClientOptions = fault.proxy.clientOptions(),
                locationChannelName = locationHelper.channelName
            )

            // Resolve the fault and ensure the new publisher works
            fault.resolve()
            val trackable = Trackable(UUID.randomUUID().toString())
            val location = locationHelper.locationUpdate(84.0, 104.0)
            PublisherMonitor.forResolvedFault(
                "[fault resolved] ensure publisher working",
                trackable = trackable,
                faultType = fault.type,
                locationUpdate = location
            ).waitForStateTransition {
                newPublisher.track(trackable).also {
                    locationHelper.sendUpdate(location)
                }
            }.close()

            /*
             * Stop the new publisher.
             * This is required because Mapbox instances are kept as a singleton and not reducing the reference
             * count to zero will result in a stale instance leaking into other tests that run subsequently if
             * garbage collection doesn't clean things up fast enough (which can lead to other tests seeing incorrect
             * location updates).
             */
            runBlocking {
                newPublisher.stop()
            }
        }
    }

    /**
     * Checks that we have TestResources initialized and executes the test body
     */
    private fun withResources(testBody: TestResources.() -> Unit) {
        val resources = testResources
        if (resources == null) {
            Assert.fail("Test has not been initialized")
        } else {
            resources.apply(testBody)
        }
    }
}

/**
 * Common test resources required by all tests above, packaged into a utility
 * class to make setup and teardown consistent and prevent the need for excessive
 * null-checking in every test implementation
 */
class TestResources(
    val context: Context,
    val locationHelper: LocationHelper,
    val fault: FaultSimulation,
    val publisher: Publisher
) {
    companion object {
        /**
         * Initialize common test resources required for all tests
         */
        fun setUp(faultParam: FaultSimulation): TestResources {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val locationHelper = LocationHelper()
            faultParam.proxy.start()
            val publisher = createPublisher(context, faultParam.proxy.clientOptions(), locationHelper.channelName)

            return TestResources(
                context = context,
                locationHelper = locationHelper,
                fault = faultParam,
                publisher = publisher
            )
        }

        /**
         * Injects a pre-configured AblyRealtime instance to the Publisher by constructing it
         * and all dependencies by hand, side-stepping the builders, which block this.
         */
        @SuppressLint("MissingPermission")
        fun createPublisher(
            context: Context,
            proxyClientOptions: ClientOptions,
            locationChannelName: String
        ): Publisher {
            val resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0)
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

            return DefaultPublisher(
                DefaultAbly(ablySdkFactory, connectionConfiguration, Logging.aatDebugLogger),
                DefaultMapbox(
                    context,
                    MapConfiguration(MAPBOX_ACCESS_TOKEN),
                    connectionConfiguration,
                    LocationSourceFlow(createAblyLocationSource(locationChannelName)),
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

        private fun createAblyLocationSource(channelName: String): Flow<Location> {
            val ably = AblyRealtime(CLIENT_OPTS_NO_PROXY)
            val simulationChannel = ably.channels.get(channelName)
            val flow = MutableSharedFlow<Location>(replay = 1)
            val scope = CoroutineScope(Dispatchers.IO)
            val gson = Gson()

            ably.connection.on { testLogD("Ably connection state change: ${it.current}") }

            val connectedToSimulationChannel = UnitExpectation("Connected to location simulation channel")
            simulationChannel.on {
                testLogD("Ably channel state change: ${it.current}")
                if (it.current == ChannelState.attached) {
                    connectedToSimulationChannel.fulfill()
                }
            }

            simulationChannel.subscribe(EventNames.ENHANCED) { message ->
                testLogD("Ably channel message: $message")
                message.getLocationMessages(gson).forEach {
                    testLogD("Received enhanced location: ${it.synopsis()}")
                    val loc = it.toTracking()

                    // TODO do we need to overwrite loc.time here?
                    // previously, for Android Location, we had:
                    //   loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

                    scope.launch {
                        flow.emit(loc)
                    }
                }
            }

            // Make sure we're on the channel before continuing
            connectedToSimulationChannel.await(5)
            connectedToSimulationChannel.assertFulfilled()

            return flow
        }
    }

    fun tearDown() {
        shutdownPublisher(publisher).assertSuccess()
        locationHelper.close()
        fault.cleanUp()
    }

    /**
     * Shutdown the given publisher and wait for confirmation, or a timeout.
     * Returns a BooleanExpectation, which can be used to check for successful
     * shutdown of the publisher
     */
    private fun shutdownPublisher(publisher: Publisher): BooleanExpectation {
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
private val CLIENT_OPTS_NO_PROXY = ClientOptions().apply {
    this.clientId = "IntegTests_NoProxy"
    this.key = BuildConfig.ABLY_API_KEY
    this.logHandler = Logging.ablyJavaDebugLogger
    // Setting log level only to prevent overwriting it in the AblyRealtime instance that we care about
    // can be removed once underlying ably-java issue is fixed - https://github.com/ably/ably-java/issues/901
    this.logLevel = Log.VERBOSE
}

/**
 * Helper class to publish basic location updates through a known Ably channel name
 */
class LocationHelper {
    private val opts = CLIENT_OPTS_NO_PROXY
    private val ably = AblyRealtime(opts)

    val channelName = "publisherIntergrationTestLocations-" + UUID.randomUUID()
    private val channel = ably.channels.get(channelName)

    private val gson = Gson()

    /**
     * Send a location update message on trackable channel and wait for confirmation
     * of publish completing successfully. Will fail the test if publishing fails.
     */
    fun sendUpdate(locationUpdate: LocationMessage) {
        val ablyMessage = Message(EventNames.ENHANCED, gson.toJson(arrayOf(locationUpdate)))
        val publishExpectation = BooleanExpectation("publishing Ably location update")
        channel.publish(
            ablyMessage,
            object : CompletionListener {
                override fun onSuccess() {
                    testLogD("Location publish success")
                    publishExpectation.fulfill(true)
                }

                override fun onError(err: ErrorInfo?) {
                    testLogD("Location publish failed: ${err?.code} - ${err?.message}")
                    publishExpectation.fulfill(false)
                }
            }
        )

        publishExpectation.await()
        publishExpectation.assertSuccess()
    }

    /**
     * Construct a fake [LocationMessage] for test purposes
     */
    fun locationUpdate(lat: Double, long: Double) =
        LocationMessage(
            type = "Feature",
            geometry = LocationGeometry(
                type = GeoJsonTypes.POINT,
                coordinates = listOf(long, lat, 0.0)
            ),
            properties = LocationProperties(
                accuracyHorizontal = 5.0f,
                bearing = 0.0f,
                speed = 5.0f,
                time = Date().time.toDouble() / 1000
            )
        )

    /**
     * Close Ably connection
     */
    fun close() {
        ably.close()
    }
}

/**
 * Monitors Publisher activity after or while performing actions on the
 * Publisher API, so that we can make assertions about any trackable state
 * transitions expected and ensure side-effects occur.
 */
class PublisherMonitor(
    val label: String,
    val trackable: Trackable,
    private val expectedState: KClass<out TrackableState>,
    private val failureStates: Set<KClass<out TrackableState>>,
    private val expectedPublisherPresence: Boolean?,
    private val expectedLocationUpdate: LocationMessage? = null,
    val timeout: Long,
) {

    private val ably = AblyRealtime(CLIENT_OPTS_NO_PROXY)
    private val gson = Gson()

    companion object {
        /**
         * Construct [PublisherMonitor] configured to expect appropriate state transitions
         * for the given fault type while it is active. [label] will be used for logging captured transitions.
         */
        fun forActiveFault(
            label: String,
            trackable: Trackable,
            faultType: FaultType,
            locationUpdate: LocationMessage? = null,
        ) = PublisherMonitor(
            label = label,
            trackable = trackable,
            expectedState = when (faultType) {
                is FaultType.Fatal -> TrackableState.Failed::class
                is FaultType.Nonfatal -> TrackableState.Online::class
                is FaultType.NonfatalWhenResolved -> TrackableState.Offline::class
            },
            failureStates = when (faultType) {
                is FaultType.Fatal -> setOf(TrackableState.Offline::class)
                is FaultType.Nonfatal, is FaultType.NonfatalWhenResolved ->
                    setOf(TrackableState.Failed::class)
            },
            expectedPublisherPresence = when (faultType) {
                is FaultType.Nonfatal -> true
                is FaultType.NonfatalWhenResolved -> null
                is FaultType.Fatal -> false
            },
            expectedLocationUpdate = when (faultType) {
                is FaultType.Nonfatal -> locationUpdate
                else -> null
            },
            timeout = when (faultType) {
                is FaultType.Fatal -> faultType.failedWithinMillis
                is FaultType.Nonfatal -> faultType.resolvedWithinMillis
                is FaultType.NonfatalWhenResolved -> faultType.offlineWithinMillis
            }
        )

        /**
         * Construct a [PublisherMonitor] configured to expect appropriate transitions for
         * the given fault type after it has been resolved. [label] is used for logging.
         */
        fun forResolvedFault(
            label: String,
            trackable: Trackable,
            faultType: FaultType,
            locationUpdate: LocationMessage? = null,
        ) = PublisherMonitor(
            label = label,
            trackable = trackable,
            expectedState = when (faultType) {
                is FaultType.Fatal -> TrackableState.Failed::class
                is FaultType.Nonfatal, is FaultType.NonfatalWhenResolved ->
                    TrackableState.Online::class
            },
            failureStates = when (faultType) {
                is FaultType.Fatal -> setOf(
                    TrackableState.Offline::class,
                    TrackableState.Online::class
                )
                is FaultType.Nonfatal, is FaultType.NonfatalWhenResolved ->
                    setOf(TrackableState.Failed::class)
            },
            expectedPublisherPresence = when (faultType) {
                is FaultType.Fatal -> false
                else -> true
            },
            expectedLocationUpdate = locationUpdate,
            timeout = when (faultType) {
                is FaultType.Fatal -> faultType.failedWithinMillis
                is FaultType.Nonfatal -> faultType.resolvedWithinMillis
                is FaultType.NonfatalWhenResolved -> faultType.offlineWithinMillis
            }
        )

        /**
         * Construct a [PublisherMonitor] configured to expect a Trackable to come
         * online within a given timeout, and fail if the Failed state is seen at any point.
         */
        fun onlineWithoutFail(
            label: String,
            trackable: Trackable,
            timeout: Long,
            locationUpdate: LocationMessage? = null,
        ) = PublisherMonitor(
            label = label,
            trackable = trackable,
            expectedState = TrackableState.Online::class,
            failureStates = setOf(TrackableState.Failed::class),
            expectedPublisherPresence = true,
            expectedLocationUpdate = locationUpdate,
            timeout = timeout
        )
    }

    /**
     * Performs the given async (suspending) operation in a runBlocking, attaching the
     * returned StateFlow<TrackableState> to the given receiver, then waits for expectations
     * to be delivered (or not) before cleaning up.
     */
    fun waitForStateTransition(
        asyncOp: suspend () -> StateFlow<TrackableState>
    ): PublisherMonitor {
        runBlocking {
            try {
                withTimeout(timeout) {
                    val trackableStateFlow = asyncOp()
                    testLogD("$label - success")

                    testLogD("Await state transition")
                    assertStateTransition(trackableStateFlow)
                    testLogD("Await assert presence")
                    assertPresence()
                    testLogD("Await assert location")
                    assertLocationUpdated()
                    testLogD("Await assert done")
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

    /**
     * Throw an assertion error of the expected [TrackableState] transition hasn't happened.
     */
    private suspend fun assertStateTransition(stateFlow: StateFlow<TrackableState>) {
        val result = stateFlow.mapNotNull { receive(it) }.first()
        if (!result) {
            throw AssertionError("Expectation '$label' did not result in success.")
        }
    }

    /**
     * Maps received [TrackableState] to a success/fail/ignore outcome for this test.
     */
    private fun receive(state: TrackableState): Boolean? =
        when {
            failureStates.contains(state::class) -> {
                testLogD("PublisherMonitor (FAIL): $label - $state")
                false
            }
            expectedState == state::class -> {
                testLogD("PublisherMonitor (SUCCESS): $label - $state")
                true
            }
            else -> {
                testLogD("PublisherMonitor (IGNORED): $label - $state")
                null
            }
        }

    /**
     * Throw an assertion error if the publisher's presence does not meet expectations for this test.
     */
    private fun assertPresence() {
        if (expectedPublisherPresence == null) {
            // not checking for publisher presence in this test
            testLogD("PublisherMonitor: $label - (SKIP) expectedPublisherPresence = null")
            return
        }

        val publisherPresent = publisherIsPresent()
        if (publisherPresent != expectedPublisherPresence) {
            testLogD("PublisherMonitor: $label - (FAIL) publisherPresent = $publisherPresent")
            throw AssertionError(
                "Expected publisherPresent: $expectedPublisherPresence but got $publisherPresent"
            )
        } else {
            testLogD("PublisherMonitor: $label - (PASS) publisherPresent = $publisherPresent")
        }
    }

    /**
     * Perform a request to the Ably API to get a snapshot of the current presence for the channel,
     * and check to see if the Publisher's clientId is present in that snapshot.
     */
    private fun publisherIsPresent() =
        ably.channels
            .get("tracking:${trackable.id}")
            ?.presence
            ?.get(true)
            ?.find {
                it.clientId == PUBLISHER_CLIENT_ID &&
                    it.action == PresenceMessage.Action.present
            } != null

    /**
     * Throw an assertion error if expectations about published location updates have not
     * been meet in this test.
     */
    private fun assertLocationUpdated() {
        if (expectedLocationUpdate == null) {
            // no expected location set - skip assertion
            testLogD("PublisherMonitor: $label - (SKIP) expectedLocationUpdate = null")
            return
        }

        try {
            runBlocking {
                // The location update may be published some time after arriving on the mapbox source channel
                withTimeout(10000) {
                    while (true) {
                        val latestMsg = ably.channels
                            .get("tracking:${trackable.id}")
                            ?.history(null)
                            ?.items()
                            ?.firstOrNull()

                        // Check the trackable channel for the expected update
                        if (latestMsg != null) {
                            val latestLocation = gson.fromJson(
                                latestMsg.data as String,
                                EnhancedLocationUpdateMessage::class.java
                            ).location

                            if (latestLocation.equalGeometry(expectedLocationUpdate)) {
                                testLogD("PublisherMonitor: $label - (PASS) lastPublishedLocation = $latestLocation")
                                return@withTimeout
                            }
                        }

                        delay(200)
                    }
                }
            }
        } catch (timeout: TimeoutException) {
            testLogD("PublisherMonitor: $label - (FAIL) did not receive expected location update")
            throw AssertionError(
                "Expected location update not received"
            )
        }
    }

    /**
     * Close any open resources used by this monitor.
     */
    fun close() {
        ably.close()
    }
}

/**
 * Compare the geometry values of two [LocationMessage]s, so that we can confirm
 * a locationUpdate has been published. We can't just use value comparison because
 * AAT modifies the timestamps before publishing.
 */
fun LocationMessage?.equalGeometry(other: LocationMessage?) =
    this?.geometry == other?.geometry
