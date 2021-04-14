package com.ably.tracking.java

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ConnectionConfigurationKey
import com.ably.tracking.ConnectionConfigurationToken
import com.ably.tracking.TokenRequest
import com.ably.tracking.TokenRequestParameters

/**
 * Static factory used to create supported [ConnectionConfiguration] from Java code.
 */
class ConnectionConfigurationFactory {
    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun createKey(apiKey: String, clientId: String): ConnectionConfigurationKey {
            return ConnectionConfigurationKey.create(apiKey, clientId)
        }

        /**
         * @param callback [TokenRequestCallback] that will be called each time a [TokenRequest] needs to be obtained.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun createToken(callback: TokenRequestCallback, clientId: String): ConnectionConfigurationToken {
            return ConnectionConfigurationToken.create({ callback.onRequestToken(it) }, clientId)
        }
    }
}

/**
 * Interface provided for those using the [ConnectionConfigurationToken] from Java code.
 */
interface TokenRequestCallback {
    /**
     * This method will be called each time a [TokenRequest] needs to be obtained.
     *
     * @param requestParameters Parameters of the token request
     * @return [TokenRequest] used to obtain the token
     */
    fun onRequestToken(requestParameters: TokenRequestParameters): TokenRequest
}
