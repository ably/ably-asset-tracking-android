package com.ably.tracking.test.android.common

import com.ably.tracking.TrackableState
import io.ktor.websocket.*
import org.msgpack.value.impl.ImmutableStringValueImpl
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
        stage: FaultSimulationStage)
    : TrackableStateReceiver

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
abstract class TransportLayerFault : FaultSimulation() {
    val tcpProxy =  Layer4Proxy()
    override val proxy = tcpProxy
}

/**
 * A Transport-layer fault implementation that breaks nothing, useful for ensuring the
 * test code works under normal proxy functionality.
 */
class NullTransportFault : TransportLayerFault() {
    override val name = "NullTransportFault"
    override fun enable() { }
    override fun resolve() { }
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * A fault implementation that will prevent the proxy from accepting TCP connections when active
 */
class TcpConnectionRefused : TransportLayerFault() {

    override val name = "TcpConnectionRefused"

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
class TcpConnectionUnresponsive : TransportLayerFault() {

    override val name = "TcpConnectionUnresponsive"

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
 * Base class for Application layer faults, which will need access to the Ably
 * WebSockets protocol, and therefore a Layer 7 proxy.
 */
abstract class ApplicationLayerFault : FaultSimulation() {
    val applicationProxy = Layer7Proxy()
    override val proxy = applicationProxy
}

/**
 * An empty fault implementation for the Layer 7 proxy to ensure that normal
 * functionality is working with no interventions
 */
class NullApplicationLayerFault : ApplicationLayerFault() {
    override val name = "NullApplicationLayerFault"
    override fun enable() { }
    override fun resolve() { }
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

//
// Ably protocol action identifiers used by interceptors
//
const val ATTACH_ACTION = 10
const val DETACH_ACTION = 12
const val PRESENCE_ACTION = 14

/**
 * Base class for all faults that simply drop messages with a specific action
 * type in a specified direction
 */
abstract class DropAction(
    private val direction: FrameDirection,
    private val action: Int
    ) : ApplicationLayerFault() {

    companion object {
        private const val tag = "DropAction"
    }

    private var initialConnection = true

    override fun enable() {
        applicationProxy.interceptor = object: Layer7Interceptor {

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
            messageAction(frame) == action
}

/**
 * A DropAction fault implementation to drop ATTACH messages,
 * simulating the Ably server failing to respond to channel attachment
 */
class AttachUnresponsive : DropAction(
    direction = FrameDirection.ClientToServer,
    action = ATTACH_ACTION
) {
    override val name = "AttachUnresponsive"
}

/**
 * A DropAction fault implementation to drop DETACH messages,
 * simulating the Ably server failing to detach a client from a channel.
 */
class DetachUnresponsive : DropAction(
    direction = FrameDirection.ClientToServer,
    action = DETACH_ACTION
) {
    override val name = "DetachUnresponsive"
}

/**
 * Abstract fault implementation to trigger the proxy to go unresponsive
 * (i.e. stop forwarding messages in a specific direction) once a particular
 * action has been seen in the given direction.
 */
abstract class UnresponsiveAfterAction(
    private val direction: FrameDirection,
    private val action: Int
) : ApplicationLayerFault() {

    companion object {
        private const val tag = "UnresponsiveAfterAction"
        private const val restoreFunctionalityAfterConnections = 2
    }

    private var nConnections = 0
    private var isTriggered = false

    override fun enable() {
        applicationProxy.interceptor = object: Layer7Interceptor {

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
            messageAction(frame) == action
}

/**
 * A DropAction fault implementation to drop PRESENCE messages,
 * simulating a presence enter failure
 */
class EnterUnresponsive : UnresponsiveAfterAction(
    direction = FrameDirection.ClientToServer,
    action = PRESENCE_ACTION
) {
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
 * Helper to capture an expected set of successful or unsuccessful TrackableState
 * transitions using the StateFlows provided by publishers.
 */
class TrackableStateReceiver(
    private val label: String,
    private val expectedStates: Set<KClass<out TrackableState>>,
    private val failureStates: Set<KClass<out TrackableState>>
) {
    val outcome = BooleanExpectation(label)

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

/**
 * Unpack the action field from the WebSocket frame using MsgPack
 */
internal fun messageAction(frame: Frame) =
    unpack(frame.data)
        ?.map()
        ?.get(ImmutableStringValueImpl("action"))
        ?.asIntegerValue()
        ?.asInt()
