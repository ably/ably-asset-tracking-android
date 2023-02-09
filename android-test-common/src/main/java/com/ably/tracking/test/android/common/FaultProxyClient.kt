package com.ably.tracking.test.android.common

import java.net.URL
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// TODO don't duplicate these, make a library instead
@Serializable
data class ProxyDto(val listenPort: Int)

@Serializable
data class FaultSimulationDto(val id: String, val type: FaultType, val proxy: ProxyDto) {}

class FaultProxyClient(
    private val baseUrl: URL
) {
    private val client = HttpClient(CIO) {
        install(Logging)
        expectSuccess = true
    }

    private fun urlForPathComponents(vararg pathComponents: String): Url {
        return URLBuilder(baseUrl.toString())
            .appendPathSegments(*pathComponents)
            .build()
    }

    suspend fun getAllFaults(): List<Fault> {
        val url = urlForPathComponents("faults")

        val response = client.get(url)
        val faultNames = Json.decodeFromString<List<String>>(response.body<String>())

        return faultNames.map { Fault(it, this) }
    }

    suspend fun createFaultSimulation(faultName: String, apiKey: String): FaultSimulation {
        val url = urlForPathComponents("faults", faultName, "simulation")

        val response = client.post(url)
        val dto = Json.decodeFromString<FaultSimulationDto>(response.body<String>())

        return FaultSimulation(dto, baseUrl.host, apiKey, this)
    }

    suspend fun enableFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "enable")

        client.post(url)
    }

    suspend fun resolveFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "resolve")

        client.post(url)
    }

    suspend fun cleanUpFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "clean-up")

        client.post(url)
    }
}
