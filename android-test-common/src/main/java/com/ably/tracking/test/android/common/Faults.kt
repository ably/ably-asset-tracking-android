package com.ably.tracking.test.android.common

import com.ably.tracking.TrackableState
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
        stage: FaultSimulationStage
    ): TrackableStateReceiver

    override fun toString() = name
}

/**
 * Steps during a fault simulation test:
 *   - FaultActive - fault.enable() has been called
 *   - FaultResolved - fault.enable() was called earlier, now fault.resolve()
 *     has also been called.
 */
enum class FaultSimulationStage {
    FaultActive,
    FaultResolved
}

/**
 * Base class for faults requiring a Layer 4 proxy for simulation.
 */
abstract class TransportFault(open val apiKey: String) : FaultSimulation() {
    val tcpProxy = Layer4Proxy(apiKey = apiKey)
    override val proxy = tcpProxy
}

/**
 * A Transport-layer fault implementation that breaks nothing, useful for ensuring the
 * test code works under normal proxy functionality.
 */
class NullTransportFault(override val apiKey: String) : TransportFault(apiKey) {
    override val name = "NullTransportFault"
    override fun enable() {}
    override fun resolve() {}
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * A fault implementation that will prevent the proxy from accepting TCP connections when active
 */
class TcpConnectionRefused(override val apiKey: String) : TransportFault(apiKey) {

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
        FaultSimulationStage.FaultActive ->
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
            TrackableStateReceiver.onlineWithoutFail("$name: $stage")
    }
}

/**
 * A fault implementation that hangs the TCP connection by preventing the Layer 4
 * proxy from forwarding packets in both directions
 */
class TcpConnectionUnresponsive(override val apiKey: String) : TransportFault(apiKey) {

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
        FaultSimulationStage.FaultActive ->
            TrackableStateReceiver.offlineWithoutFail("$name: $stage")
        FaultSimulationStage.FaultResolved ->
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
