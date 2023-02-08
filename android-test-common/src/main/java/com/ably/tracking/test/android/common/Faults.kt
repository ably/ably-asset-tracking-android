package com.ably.tracking.test.android.common

import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * A simple factory interface to build new instances of a specific [FaultSimulation]
 * This is needed because JUnit parameterized tests won't construct fresh instances
 * of data parameters for each test - they wouldn't know how, without a factory.
 */
class Fault(
    /**
     * A human-readable name for this type of fault
     */
    val name: String,

    private val client: FaultProxyClient
) {
    /**
     * Create a fresh simulation of this fault type, using provided Ably credentials
     */
    suspend fun simulate(apiKey: String): FaultSimulation {
        val dto = client.createFaultSimulation(faultName = name)
        return FaultSimulation(dto, apiKey, client)
    }

    override fun toString() = name
}

/**
 * Abstract interface definition for specific instances of connectivity
 * faults that can occur. Implementations should provide a proxy that they
 * are able to break and resolve according to their own fault criteria.
 *
 * Faults should also specify what state Trackables should enter during a fault
 * and after it has been resolved, so that assertions can be made while testing
 * common use-cases.
 */
class FaultSimulation(
    private val dto: FaultSimulationDto,
    private val apiKey: String,
    private val client: FaultProxyClient
) {
    val proxy: RealtimeProxy
    val type: FaultType

    init {
        proxy = RealtimeProxy(this.dto.proxy, apiKey)
        type = dto.type
    }

    // TODO decide how to address this
    val skipTest = false

    /**
     * Break the proxy using the fault-specific failure conditions
     */
    suspend fun enable() {
        client.enableFaultSimulation(dto.id)
    }

    /**
     * Restore the proxy to normal functionality
     */
    suspend fun resolve() {
        client.resolveFaultSimulation(dto.id)
    }

    /**
     * Called at start of test tearDown function to ensure fault doesn't interefere with test
     * tearDown of open resources.
     */
    suspend fun cleanUp() {
        client.cleanUpFaultSimulation(dto.id)
    }
}

/**
 * Describes the nature of a given fault simulation, and specifically the impact that it
 * should have on any Trackables or channel activity during and after resolution.
 */
sealed class FaultType {
    /**
     * AAT and/or Ably Java should handle this fault seamlessly Trackable state should be
     * online and publisher should be present within [resolvedWithinMillis]. It's possible
     * the fault will cause a brief Offline blip, but tests should expect to see Trackables
     * Online again before [resolvedWithinMillis] expires regardless.
     */
    data class Nonfatal(
        val resolvedWithinMillis: Long,
    ) : FaultType()

    /**
     * This is a non-fatal error, but will persist until the [FaultSimulation.resolve]
     * method has been called. Trackable states should be offline during the fault within
     * [offlineWithinMillis] maximum. When the fault is resolved, Trackables should return
     * online within [onlineWithinMillis] maximum.
     *
     */
    data class NonfatalWhenResolved(
        val offlineWithinMillis: Long,
        val onlineWithinMillis: Long,
    ) : FaultType()

    /**
     * This is a fatal error and should permanently move Trackables to the Failed state.
     * The publisher should not be present in the corresponding channel any more and no
     * further location updates will be published. Tests should check that Trackables reach
     * the Failed state within [failedWithinMillis]
     */
    data class Fatal(
        val failedWithinMillis: Long,
    ) : FaultType()
}
