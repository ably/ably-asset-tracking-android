package com.ably.tracking.test.android.common

import java.net.URI

// TODO don't duplicate these, make a library instead
@Serializable
data class ProxyDto(val listenHost: String, val listenPort: Int)

@Serializable
data class FaultSimulationDto(val id: String, val type: FaultType, val proxy: ProxyDto) {}

class FaultProxyClient(
    private val baseUri: URI
) {
    suspend fun getAllFaults(): List<Fault> {
        // TODO
    }

    suspend fun createFaultSimulation(faultName: String): FaultSimulationDto {
        // TODO
    }

    suspend fun enableFaultSimulation(id: String) {
        // TODO
    }

    suspend fun resolveFaultSimulation(id: String) {
        // TODO
    }

    suspend fun cleanUpFaultSimulation(id: String) {
        // TODO
    }
}
