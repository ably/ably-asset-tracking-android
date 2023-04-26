package com.ably.tracking.test.android.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        return client.createFaultSimulation(name, apiKey)
    }

    override fun toString() = name
}

/**
 * Represents a specific instance of a connectivity fault that can occur.
 *
 * Specifies what state Trackables should enter during a fault and after it has
 * been resolved, so that assertions can be made while testing common
 * use-cases.
 */
class FaultSimulation(
    private val dto: FaultSimulationDto,
    proxyHost: String,
    apiKey: String,
    private val client: FaultProxyClient
) {
    val proxy: RealtimeProxy
    val type: FaultType

    init {
        proxy = RealtimeProxy(this.dto.proxy, proxyHost, apiKey)
        type = dto.type
    }

    /**
     * Add a fault’s name to this list in order to skip tests that use this fault.
     * We're using this in order to allow us to write tests which are known to fail, then allow them to pass in the CI
     * environment temporarily until we subsequently raise a pull request to fix them.
     * The advantage of this approach is that the test code remains active and continually compiled as
     * a first class citizen of the codebase, while we work on other things to get it passing.
     */
    private val faultNamesSkippedForPublisherTest = listOf<String>()

    val skipPublisherTest = faultNamesSkippedForPublisherTest.contains(dto.name)

    // Causes the same behaviour as faultNamesSkippedForPublisherTest for the subscriber NetworkConnectivityTests
    private val faultNamesSkippedForSubscriberTest = listOf(
        "TcpConnectionRefused",
        "DisconnectAndSuspend",
        "EnterUnresponsive",
        "ReenterOnResumeFailed"
    )

    val skipSubscriberTest = faultNamesSkippedForSubscriberTest.contains(dto.name)

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
@Serializable
sealed class FaultType {
    /**
     * AAT and/or Ably Java should handle this fault seamlessly Trackable state should be
     * online and publisher should be present within [resolvedWithinMillis]. It's possible
     * the fault will cause a brief Offline blip, but tests should expect to see Trackables
     * Online again before [resolvedWithinMillis] expires regardless.
     */
    @Serializable
    @SerialName("nonfatal")
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
    @Serializable
    @SerialName("nonfatalWhenResolved")
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
    @Serializable
    @SerialName("fatal")
    data class Fatal(
        val failedWithinMillis: Long,
    ) : FaultType()
}
