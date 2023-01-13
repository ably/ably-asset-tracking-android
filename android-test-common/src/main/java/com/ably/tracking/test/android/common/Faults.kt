package com.ably.tracking.test.android.common

import com.ably.tracking.TrackableState
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
abstract class TransportFault(apiKey: String) : FaultSimulation() {
    val tcpProxy = Layer4Proxy(apiKey = apiKey)
    override val proxy = tcpProxy
}

/**
 * A Transport-layer fault implementation that breaks nothing, useful for ensuring the
 * test code works under normal proxy functionality.
 */
class NullTransportFault(apiKey: String) : TransportFault(apiKey) {
    override val name = "NullTransportFault"
    override fun enable() {}
    override fun resolve() {}
    override fun stateReceiverForStage(stage: FaultSimulationStage) =
        TrackableStateReceiver.onlineWithoutFail("$name: $stage")
}

/**
 * A fault implementation that will prevent the proxy from accepting TCP connections when active
 */
class TcpConnectionRefused(apiKey: String) : TransportFault(apiKey) {

    override val name = "TcpConnectionRefused"

    /**
     * This fault type is temporarily disabled at runtime. It can be re-enabled by removing this override.
     * We will re-enable this test when the following have been addressed:
     * - https://github.com/ably/ably-asset-tracking-android/issues/859
     * - https://github.com/ably/ably-asset-tracking-android/issues/871
     */
    override val skipTest = true

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
class TcpConnectionUnresponsive(apiKey: String) : TransportFault(apiKey) {

    override val name = "TcpConnectionUnresponsive"

    /**
     * This fault type is temporarily disabled at runtime. It can be re-enabled by removing this override.
     * We will re-enable this test when the following have been addressed:
     * - https://github.com/ably/ably-asset-tracking-android/issues/859
     * - https://github.com/ably/ably-asset-tracking-android/issues/871
     */
    override val skipTest = true

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
