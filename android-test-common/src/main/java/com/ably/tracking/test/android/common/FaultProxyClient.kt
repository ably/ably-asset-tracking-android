package com.ably.tracking.test.android.common

import com.ably.tracking.test.android.common.Logging.testLogD
import java.net.URL
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.ktor.client.call.body

@Serializable
data class ProxyDto(val listenPort: Int)

@Serializable
data class FaultSimulationDto(val id: String, val name: String, val type: FaultType, val proxy: ProxyDto)

/**
 * A client for communicating with an instance of the SDK test proxy server. Provides methods for creating and managing proxies which are able to simulate connectivity faults that might occur during use of the Ably Asset Tracking SDKs.
 */
class FaultProxyClient(
    /* 10.0.2.2 is the loopback interface of the host machine the emulator is running on:
     * https://developer.android.com/studio/run/emulator-networking.html#networkaddresses
     */
    private val baseUrl: URL = URL("http://10.0.2.2:8080")
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

    /**
     * Lists all of the faults that the server is capable of simulating.
     */
    suspend fun getAllFaults(): List<Fault> {
        val url = urlForPathComponents("faults")

        val response = client.get(url)
        val faultNames = Json.decodeFromString<List<String>>(response.body<String>())

        return faultNames.map { Fault(it, this) }
    }

    /**
     * Creates a fault simulation and starts its proxy.
     */
    suspend fun createFaultSimulation(faultName: String, apiKey: String): FaultSimulation {
        val url = urlForPathComponents("faults", faultName, "simulation")

        val response = client.post(url)
        val dto = Json.decodeFromString<FaultSimulationDto>(response.body<String>())

        testLogD("Created fault simulation $dto")

        return FaultSimulation(dto, baseUrl.host, apiKey, this)
    }

    /**
     * Breaks the proxy using the fault-specific failure conditions.
     */
    suspend fun enableFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "enable")

        client.post(url)
    }

    /**
     * Restores the proxy to normal functionality.
     */
    suspend fun resolveFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "resolve")

        client.post(url)
    }

    /**
     * Stops the proxy. This should be called at the end of each test case that creates a fault simulation.
     */
    suspend fun cleanUpFaultSimulation(id: String) {
        val url = urlForPathComponents("fault-simulations", id, "clean-up")

        client.post(url)
    }
}
