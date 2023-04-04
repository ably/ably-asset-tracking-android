package com.ably.tracking.test.android.common

import io.ably.lib.types.ClientOptions
import io.ably.lib.util.Log

private const val AGENT_HEADER_NAME = "ably-asset-tracking-android-publisher-tests"

const val PUBLISHER_CLIENT_ID = "AatNetworkConnectivityTests_Publisher"
const val SUBSCRIBER_CLIENT_ID = "AatNetworkConnectivityTests_Subscriber"

/**
 * The details of a proxy that can be used to intercept Realtime traffic for testing
 */
class RealtimeProxy(
    private val dto: ProxyDto,
    private val host: String,
    private val apiKey: String
) {

    /**
     * Ably publisher ClientOptions that have been configured to direct traffic
     * through this proxy service
     */
    fun publisherClientOptions() = clientOptions(PUBLISHER_CLIENT_ID)

    /**
     * Ably subscriber ClientOptions that have been configured to direct traffic
     * through this proxy service
     */
    fun subscriberClientOptions() = clientOptions(SUBSCRIBER_CLIENT_ID)

    private fun clientOptions(clientId: String) = ClientOptions().apply {
        this.clientId = clientId
        this.agents = mapOf(AGENT_HEADER_NAME to BuildConfig.VERSION_NAME)
        this.idempotentRestPublishing = true
        this.autoConnect = false
        this.key = apiKey
        this.logHandler = Log.LogHandler { _, _, msg, tr ->
            testLogD("${msg!!} - $tr", tr)
        }
        this.logLevel = Log.VERBOSE
        this.realtimeHost = host
        this.port = dto.listenPort
        this.tls = false
    }
}
