package com.ably.tracking.test.android.common

import com.ably.tracking.TrackableState
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.reflect.KClass

/**
 * Abstract interface definition for specific instances of connectivity
 * faults that can occur. Implementations should provide a proxy that they
 * are able to break and resolve according to their own fault criteria.
 *
 * Faults should also specify what state Trackables should enter during a fault
 * and after it has been resolved, so that assertions can be made while testing
 * common use-cases.
 */
abstract class FaultSimulation {
    /**
     * A human-readable name for this type of fault
     */
    abstract val name: String

    /**
     * Subclasses can override this value to `true` in order to skip test that use this fault.
     * We're using this in order to allow us to write tests which are known to fail, then allow them to pass in the CI
     * environment temporarily until we subsequently raise a pull request to fix them.
     * The advantage of this approach is that the test code remains active and continually compiled as
     * a first class citizen of the codebase, while we work on other things to get it passing.
     */
    open val skipTest: Boolean = false

    // As per skipTest, but only skips on subscriber tests.
    open val skipSubscriberTest: Boolean = false

    /**
     * A RealtimeProxy instance that will be manipulated by this fault
     */
    abstract val proxy: RealtimeProxy

    /**
     * Break the proxy using the fault-specific failure conditions
     */
    abstract fun enable()

    /**
     * Restore the proxy to normal functionality
     */
    abstract fun resolve()

    /**
     * Provide a TrackableStateReceiver describing the acceptable state transitions
     * during the given fault stage.
     */
    abstract fun stateReceiverForStage(
        stage: FaultSimulationStage
    ): TrackableStateReceiver

    override fun toString() = name
}

/**
 * Steps during a fault simulation test:
 *   - FaultActiveBeforeTracking - fault.enable() has been called but trackables
 *     have not yet been tracked, so attempts to track will follow
 *   - FaultActiveDuringTracking - fault.enable() has been called while trackables
 *     were already online
 *   - FaultResolved - fault.enable() was called earlier, now fault.resolve()
 *     has also been called.
 */
enum class FaultSimulationStage {
    FaultActiveBeforeTracking,
    FaultActiveDuringTracking,
    FaultResolved
}

/**
 * Base class for faults requiring a Layer 4 proxy for simulation.
 */
abstract class TransportLayerFault(apiKey: String) : FaultSimulation() {
    val tcpProxy = Layer4Proxy(apiKey = apiKey)
    override val proxy = tcpProxy
}

/**
 * A Transport-layer fault implementation that breaks nothing, useful for ensuring the
 * test code works under normal proxy functionality.
 */
class NullTransportFault(apiKey: String) : TransportLayerFault(apiKey) {
    override val name = "NullTransportFault"
    override fun enable() {}
    override fun resolve() {}
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * A fault implementation that will prevent the proxy from accepting TCP connections when active
 */
class TcpConnectionRefused(apiKey: String) : TransportLayerFault(apiKey) {

    override val name = "TcpConnectionRefused"

    /**
     * This fault type is temporarily disabled at runtime. It can be re-enabled by removing this override.
     * We will re-enable this test when the following have been addressed:
     * - https://github.com/ably/ably-asset-tracking-android/issues/859
     * - https://github.com/ably/ably-asset-tracking-android/issues/871
     */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    override fun enable() {
        tcpProxy.stop()
    }

    override fun resolve() {
        tcpProxy.start()
    }

    override fun stateReceiverForStage(
        stage: FaultSimulationStage
    ) = when (stage) {
        FaultSimulationStage.FaultActiveBeforeTracking,
        FaultSimulationStage.FaultActiveDuringTracking ->
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }
}

/**
 * A fault implementation that hangs the TCP connection by preventing the Layer 4
 * proxy from forwarding packets in both directions
 */
class TcpConnectionUnresponsive(apiKey: String) : TransportLayerFault(apiKey) {

    override val name = "TcpConnectionUnresponsive"

    /**
     * This fault type is temporarily disabled at runtime. It can be re-enabled by removing this override.
     * We will re-enable this test when the following have been addressed:
     * - https://github.com/ably/ably-asset-tracking-android/issues/859
     * - https://github.com/ably/ably-asset-tracking-android/issues/871
     */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    override fun enable() {
        tcpProxy.isForwarding = false
    }

    override fun resolve() {
        tcpProxy.isForwarding = true
    }

    override fun stateReceiverForStage(
        stage: FaultSimulationStage
    ) = when (stage) {
        FaultSimulationStage.FaultActiveBeforeTracking,
        FaultSimulationStage.FaultActiveDuringTracking ->
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }
}

/**
 * Fault implementation that causes the proxy to reject incoming connections entirely
 * for two minutes, then comes back online. This should force client side
 */
class DisconnectAndSuspend(apiKey: String) : TransportLayerFault(apiKey) {

    /*
        Currently failing due to Issues #871 and #907
    */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    companion object {
        const val SUSPEND_DELAY_MILLIS: Long = 2 * 60 * 1000
    }

    private val timer = Timer()

    override val name = "DisconnectAndSuspend"

    override fun enable() {
        tcpProxy.stop()
        timer.schedule(
            timerTask {
                tcpProxy.start()
            },
            SUSPEND_DELAY_MILLIS
        )
    }

    override fun resolve() {
        timer.cancel()
        tcpProxy.start()
    }

    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        // After two minutes, trackables should always return to online state
        // with no fatal error
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * Base class for Application layer faults, which will need access to the Ably
 * WebSockets protocol, and therefore a Layer 7 proxy.
 */
abstract class ApplicationLayerFault(apiKey: String) : FaultSimulation() {
    val applicationProxy = Layer7Proxy(apiKey)
    override val proxy = applicationProxy
}

/**
 * An empty fault implementation for the Layer 7 proxy to ensure that normal
 * functionality is working with no interventions
 */
class NullApplicationLayerFault(apiKey: String) : ApplicationLayerFault(apiKey) {
    override val name = "NullApplicationLayerFault"
    override fun enable() { }
    override fun resolve() { }
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * Base class for all faults that simply drop messages with a specific action
 * type in a specified direction
 */
abstract class DropAction(
    apiKey: String,
    private val direction: FrameDirection,
    private val action: Message.Action
) : ApplicationLayerFault(apiKey) {

    companion object {
        private const val tag = "DropAction"
    }

    private var initialConnection = true

    override fun enable() {
        applicationProxy.interceptor = object : Layer7Interceptor {

            override fun interceptConnection(params: ConnectionParams): ConnectionParams {
                if (initialConnection) {
                    initialConnection = false
                } else {
                    testLogD("$tag: second connection -- resolving fault")
                    resolve()
                }

                return params
            }

            override fun interceptFrame(direction: FrameDirection, frame: Frame) =
                if (shouldFilter(direction, frame)) {
                    testLogD("$tag: dropping: $direction - ${logFrame(frame)}")
                    listOf()
                } else {
                    testLogD("$tag: keeping: $direction - ${logFrame(frame)}")
                    listOf(Action(direction, frame))
                }
        }
    }

    override fun resolve() {
        applicationProxy.interceptor = PassThroughInterceptor()
        initialConnection = true
    }

    override fun stateReceiverForStage(
        stage: FaultSimulationStage
    ) = when (stage) {
        FaultSimulationStage.FaultActiveDuringTracking ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultActiveBeforeTracking ->
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }

    /**
     * Check whether this frame and direction messages the fault specification
     */
    private fun shouldFilter(direction: FrameDirection, frame: Frame) =
        frame.frameType == FrameType.BINARY &&
            direction == this.direction &&
            frame.data.unpack().isAction(action)
}

/**
 * A DropAction fault implementation to drop ATTACH messages,
 * simulating the Ably server failing to respond to channel attachment
 */
class AttachUnresponsive(apiKey: String) : DropAction(
    apiKey = apiKey,
    direction = FrameDirection.ClientToServer,
    action = Message.Action.ATTACH
) {

    /*
        Currently failing due to Issue #871 -- throwing ConnectionError
        when trying to add new trackables while offline.
     */
    override val skipTest = true

    // Test appears to crash JVM
    override val skipSubscriberTest = true

    override val name = "AttachUnresponsive"
}

/**
 * A DropAction fault implementation to drop DETACH messages,
 * simulating the Ably server failing to detach a client from a channel.
 */
class DetachUnresponsive(apiKey: String) : DropAction(
    apiKey = apiKey,
    direction = FrameDirection.ClientToServer,
    action = Message.Action.DETACH
) {
    override val name = "DetachUnresponsive"

    override val skipSubscriberTest = true
}

/**
 * Abstract fault implementation to trigger the proxy to go unresponsive
 * (i.e. stop forwarding messages in a specific direction) once a particular
 * action has been seen in the given direction.
 */
abstract class UnresponsiveAfterAction(
    apiKey: String,
    private val direction: FrameDirection,
    private val action: Message.Action
) : ApplicationLayerFault(apiKey) {

    companion object {
        private const val tag = "UnresponsiveAfterAction"
        private const val restoreFunctionalityAfterConnections = 2
    }

    private var nConnections = 0
    private var isTriggered = false

    override fun enable() {
        applicationProxy.interceptor = object : Layer7Interceptor {

            override fun interceptConnection(params: ConnectionParams): ConnectionParams {
                nConnections += 1
                if (nConnections >= restoreFunctionalityAfterConnections) {
                    testLogD("$tag: resolved after $restoreFunctionalityAfterConnections connections")
                    resolve()
                }

                return params
            }

            override fun interceptFrame(direction: FrameDirection, frame: Frame): List<Action> {
                if (shouldActivate(direction, frame)) {
                    testLogD("$tag: $name - connection going unresponsive")
                    isTriggered = true
                }

                return if (isTriggered) {
                    testLogD("$tag: $name unresponsive: dropping ${logFrame(frame)}")
                    listOf()
                } else {
                    listOf(Action(direction, frame))
                }
            }
        }
    }

    override fun resolve() {
        applicationProxy.interceptor = PassThroughInterceptor()
        isTriggered = false
        nConnections = 0
    }

    private fun shouldActivate(direction: FrameDirection, frame: Frame) =
        frame.frameType == FrameType.BINARY &&
            direction == this.direction &&
            frame.data.unpack().isAction(action)
}

/**
 * A DropAction fault implementation to drop PRESENCE messages,
 * simulating a presence enter failure
 */
class EnterUnresponsive(apiKey: String) : UnresponsiveAfterAction(
    apiKey = apiKey,
    direction = FrameDirection.ClientToServer,
    action = Message.Action.PRESENCE
) {

    /*
        This test currently fails because the ably-java hangs the client
        waiting for a presence response if there's there's a reconnection
        before successful completion of enter()
    */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    override val name = "EnterUnresponsive"

    override fun stateReceiverForStage(
        stage: FaultSimulationStage
    ) = when (stage) {
        FaultSimulationStage.FaultActiveDuringTracking ->
            // There won't be a presence.enter() during tracking
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultActiveBeforeTracking ->
            // presence.enter() when trackable added will trigger fault
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
            // always return to online state when there's no fault
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }
}

/**
 * Fault to simulate a resume failure after a reconnection. The fault implementation
 * first interrupts a connection once it sees a CONNECTED response returned from the server
 * by injecting a WebSocket CLOSE frame, disconnecting the client. When the client reconnects,
 * it intercepts the resume= parameter in the connection URL to substitute it for a fake
 * connectionId, causing the resume attempt to fail. This should not be a fatal error, the
 * Publisher should continue regardless.
 */
class DisconnectWithFailedResume(apiKey: String) : ApplicationLayerFault(apiKey) {

    /*
        Currently failing due to ably-java#474 presence bug
     */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    /**
     * State of the fault, used to control whether we're intercepting
     * the connection or looking to inject a WebSocket CLOSE at an appropriate time
     */
    private enum class State {
        AwaitingInitialConnection,
        AwaitingDisconnect,
        Reconnected
    }
    private var state = State.AwaitingInitialConnection

    override val name = "DisconnectWithFailedResume"

    override fun enable() {
        applicationProxy.interceptor = object : Layer7Interceptor {

            override fun interceptConnection(params: ConnectionParams): ConnectionParams {
                return when (state) {
                    State.AwaitingInitialConnection -> {
                        state = State.AwaitingDisconnect
                        testLogD("$name: transitioning to $state, connection params: $params")
                        params
                    }
                    State.AwaitingDisconnect -> {
                        state = State.Reconnected
                        params.copy(resume = modifyResumeParam(params.resume)).also {
                            testLogD("$name: transitioning to $state, connection params: $it")
                        }
                    }
                    State.Reconnected -> params
                }
            }

            override fun interceptFrame(direction: FrameDirection, frame: Frame): List<Action> {
                return when (state) {
                    State.AwaitingDisconnect ->
                        if (shouldDisconnect(direction, frame)) {
                            // Inject a CLOSE frame to kill the client connection now
                            listOf(Action(direction, frame), Action(direction, Frame.Close(), sendAndClose = true))
                        } else {
                            // Pass through
                            listOf(Action(direction, frame))
                        }
                    State.AwaitingInitialConnection,
                    State.Reconnected ->
                        // Always pass through in these states
                        listOf(Action(direction, frame))
                }
            }
        }
    }

    override fun resolve() {
        state = State.AwaitingInitialConnection
        applicationProxy.interceptor = PassThroughInterceptor()
    }

    override fun stateReceiverForStage(
        stage: FaultSimulationStage
    ) = when (stage) {
        // This fault is entirely non-fatal. AAT should recover to online
        // state eventually without failure at any stage in test
        FaultSimulationStage.FaultActiveDuringTracking,
        FaultSimulationStage.FaultActiveBeforeTracking,
        FaultSimulationStage.FaultResolved ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }

    /**
     * Replace the connectionId component of a connectionKey with a fake
     */
    private fun modifyResumeParam(resume: String?) =
        resume?.replace("^(.*!).*(-.*$)".toRegex()) { match ->
            "${match.groups[1]?.value}FakeFakeFakeFake${match.groups[2]?.value}"
        }

    /**
     * Check to see if the incoming message should trigger a disconnection
     */
    private fun shouldDisconnect(direction: FrameDirection, frame: Frame) =
        direction == FrameDirection.ServerToClient &&
            frame.frameType == FrameType.BINARY &&
            frame.data.unpack().isAction(Message.Action.CONNECTED)
}

/**
 * Abstract fault implementation to intercept outgoing presence messages, preventing
 * them from reaching Ably, and injecting NACK responses as replies to the client.
 */
abstract class PresenceNackFault(
    apiKey: String,
    private val nackedPresenceAction: Message.PresenceAction,
    private val response: (msgSerial: Int) -> Map<String?, Any?>,
    private val nackLimit: Int = 3
) : ApplicationLayerFault(apiKey) {

    private var nacksSent = 0

    override fun enable() {
        applicationProxy.interceptor = object : Layer7Interceptor {

            override fun interceptConnection(params: ConnectionParams) = params

            override fun interceptFrame(direction: FrameDirection, frame: Frame): List<Action> {
                return if (shouldNack(direction, frame)) {
                    val msg = frame.data.unpack()
                    testLogD("$name: will nack ($nacksSent): $msg")

                    val msgSerial = msg["msgSerial"] as Int
                    val nackFrame = Frame.Binary(true, response(msgSerial).pack())
                    testLogD("$name: sending nack: ${nackFrame.data.unpack()}")
                    nacksSent += 1

                    listOf(
                        // note: not sending this presence message to server
                        Action(FrameDirection.ServerToClient, nackFrame)
                    )
                } else {
                    listOf(Action(direction, frame))
                }
            }
        }
    }

    override fun resolve() {
        applicationProxy.interceptor = PassThroughInterceptor()
    }

    /**
     * Check whether this frame (and direction) is the kind we're trying to
     * simulate a server NACK response for
     */
    private fun shouldNack(direction: FrameDirection, frame: Frame) =
        nacksSent < nackLimit &&
            frame.frameType == FrameType.BINARY &&
            direction == FrameDirection.ClientToServer &&
            frame.data.unpack().let {
                it.isAction(Message.Action.PRESENCE) &&
                    it.isPresenceAction(nackedPresenceAction)
            }
}

/**
 * Simulates retryable presence.enter() failure. Will stop
 * nacking after 3 failures
 */
class EnterFailedWithNonfatalNack(apiKey: String) : PresenceNackFault(
    apiKey = apiKey,
    nackedPresenceAction = Message.PresenceAction.ENTER,
    response = ::nonFatalNack,
    nackLimit = 3
) {

    /*
        Currently failing due to Issue #907 - non-fatal nack triggers
        an exception to be thrown to caller during publisher.track()
     */
    override val skipTest = true

    // Can probably be removed once skipTest issues are resolved
    override val skipSubscriberTest = true

    override val name = "EnterFailedWithNonfatalNack"

    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        // Note: 5xx presence errors should always be non-fatal and recovered seamlessly
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * Simulates a retryable presence.update() failure. Will stop
 * nacking after 3 failures
 */
class UpdateFailedWithNonfatalNack(apiKey: String) : PresenceNackFault(
    apiKey = apiKey,
    nackedPresenceAction = Message.PresenceAction.UPDATE,
    response = ::nonFatalNack,
    nackLimit = 3
) {
    override val name = "UpdateFailedWithNonfatalNack"

    override val skipSubscriberTest = true

    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        // Note: 5xx presence errors should always be non-fatal and recovered seamlessly
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * Simulates a client being disconnected while present on a channel,
 * reconnecting with a successful resume, but the presence re-enter
 * failing. Client should handle this by re-entering presence when
 * it sees that re-enter has failed.
 */
class ReenterOnResumeFailed(apiKey: String) : ApplicationLayerFault(apiKey) {

    /*
       This test currently fails because the ably-java hangs the client
       waiting for a presence response if there's there's a reconnection
       before successful completion of enter()

       This happens during stage 2 of the test, so steps 3 and 4 have not
       yet been seen to work.
     */
    override val skipTest = true

    // May be able to be removed once the issues surrounding skipTest are resolved
    override val skipSubscriberTest = true

    override val name = "ReenterOnResumeFailed"

    private var state = State.DisconnectAfterPresence
    private var presenceEnterSerial: Int? = null

    private enum class State {
        // Waiting for client presence enter before forcing disconnect
        DisconnectAfterPresence,

        // Wait for server SYNC message, remove client from presence member
        InterceptingServerSync,

        // Wait for client to respond by re-entering, note the msgSerial
        InterceptingClientEnter,

        // Wait for server to ACK the enter, swap it for a NACK
        InterceptingServerAck,

        // Finished - return to normal pass-through
        WorkingNormally
    }

    override fun enable() {
        applicationProxy.interceptor = object : Layer7Interceptor {

            override fun interceptConnection(params: ConnectionParams): ConnectionParams {
                testLogD("$name: [$state] new connection: $params")
                if (state == State.DisconnectAfterPresence) {
                    state = State.InterceptingServerSync
                }
                return params
            }

            override fun interceptFrame(direction: FrameDirection, frame: Frame): List<Action> {
                return when (state) {
                    State.DisconnectAfterPresence -> disconnectAfterPresence(direction, frame)
                    State.InterceptingServerSync -> interceptServerSync(direction, frame)
                    State.InterceptingClientEnter -> interceptClientEnter(direction, frame)
                    State.InterceptingServerAck -> interceptServerAck(direction, frame)
                    State.WorkingNormally -> listOf(Action(direction, frame))
                }
            }
        }
    }

    override fun resolve() {
        state = State.DisconnectAfterPresence
        applicationProxy.interceptor = PassThroughInterceptor()
    }

    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        // This fault should not be non-fatal, Trackables should
        // return to online state before resolve() is called
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")

    /**
     * Step 1: disconnect the client to cause it to attempt a resume,
     * but wait until we know the client has attempted to enter presence
     * beforehand.
     */
    private fun disconnectAfterPresence(
        direction: FrameDirection,
        frame: Frame
    ): List<Action> {
        return if (
            direction == FrameDirection.ClientToServer &&
            frame.frameType == FrameType.BINARY &&
            frame.data.unpack().isAction(Message.Action.PRESENCE)
        ) {
            testLogD("$name: [$state] forcing disconnect")
            // Note: state will advance in interceptConnection
            listOf(
                Action(direction, frame),
                Action(FrameDirection.ServerToClient, Frame.Close(), true)
            )
        } else {
            listOf(Action(direction, frame))
        }
    }

    /**
     * Step 2: Now that the client has reconnected, tamper with the
     * incoming server SYNC message to remove this client's presence
     * data, causing it to think that re-enter on resume has failed.
     */
    private fun interceptServerSync(
        direction: FrameDirection,
        frame: Frame
    ): List<Action> {
        return if (
            direction == FrameDirection.ServerToClient &&
            frame.frameType == FrameType.BINARY &&
            frame.data.unpack().isAction(Message.Action.SYNC)
        ) {
            testLogD("$name: [$state] intercepting sync")
            state = State.InterceptingClientEnter
            listOf(
                Action(direction, removePresenceFromSync(frame))
            )
        } else {
            listOf(Action(direction, frame))
        }
    }

    /**
     * Step 3: The client should respond to the failed presence re-enter
     * on resume by attempting to enter presence again. Note the msgSerial
     * of the outgoing presence message so that we can intercept the ACK
     */
    private fun interceptClientEnter(
        direction: FrameDirection,
        frame: Frame
    ): List<Action> {
        if (direction == FrameDirection.ClientToServer &&
            frame.frameType == FrameType.BINARY
        ) {
            val msg = frame.data.unpack()
            if (msg.isAction(Message.Action.PRESENCE) &&
                msg.isPresenceAction(Message.PresenceAction.ENTER)
            ) {
                presenceEnterSerial = msg["msgSerial"] as Int
                testLogD("$name: [$state] presence enter serial: $presenceEnterSerial")
                state = State.InterceptingServerAck
            }
        }

        return listOf(Action(direction, frame))
    }

    /**
     * Step 4: Replace server ACK response with a NACK for the same msgSerial
     */
    private fun interceptServerAck(
        direction: FrameDirection,
        frame: Frame
    ): List<Action> {
        return if (
            direction == FrameDirection.ServerToClient &&
            frame.frameType == FrameType.BINARY &&
            frame.data.unpack().let {
                it.isAction(Message.Action.ACK) &&
                    (it["msgSerial"] as Int) == presenceEnterSerial
            }
        ) {
            val nack = Message.nack(
                msgSerial = presenceEnterSerial!!,
                count = 1,
                errorCode = 50000,
                errorStatusCode = 500,
                errorMessage = "injected by proxy"
            )
            testLogD("$name: [$state] sending nack: $nack")
            state = State.WorkingNormally
            listOf(Action(direction, Frame.Binary(true, nack.pack())))
        } else {
            listOf(Action(direction, frame))
        }
    }

    /**
     * Remove presence data, causing client to believe re-enter
     * on resume has failed
     */
    private fun removePresenceFromSync(frame: Frame): Frame {
        val syncMsg = frame.data.unpack().toMutableMap()
        syncMsg["presence"] = listOf<Any?>()
        return Frame.Binary(true, syncMsg.pack())
    }
}

/**
 * A non-fatal nack response for given message serial
 */
internal fun nonFatalNack(msgSerial: Int) =
    Message.nack(
        msgSerial = msgSerial,
        count = 1,
        errorCode = 50000,
        errorStatusCode = 500,
        errorMessage = "injected by proxy"
    )

/**
 * Helper to capture an expected set of successful or unsuccessful TrackableState
 * transitions using the StateFlows provided by publishers.
 */
class TrackableStateReceiver(
    private val label: String,
    private val expectedStates: Set<KClass<out TrackableState>>,
    private val failureStates: Set<KClass<out TrackableState>>
) {
    companion object {
        fun onlineWithoutFail(label: String) = TrackableStateReceiver(
            label,
            setOf(TrackableState.Online::class),
            setOf(TrackableState.Failed::class)
        )

        fun offlineWithoutFail(label: String) = TrackableStateReceiver(
            label,
            setOf(TrackableState.Offline::class),
            setOf(TrackableState.Failed::class)
        )
    }

    suspend fun assertStateTransition(stateFlow: StateFlow<TrackableState>) {
        val result = stateFlow.mapNotNull { receive(it) }.first()
        if (!result) {
            throw AssertionError("Expectation '$label' did not result in success.")
        }
    }

    private fun receive(state: TrackableState): Boolean? =
        when {
            failureStates.contains(state::class) -> {
                testLogD("TrackableStateReceived (FAIL): $label - $state")
                false
            }
            expectedStates.contains(state::class) -> {
                testLogD("TrackableStateReceived (SUCCESS): $label - $state")
                true
            }
            else -> {
                testLogD("TrackableStateReceived (IGNORED): $label - $state")
                null
            }
        }
}
