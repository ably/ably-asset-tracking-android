package com.ably.tracking.java

import com.ably.tracking.connection.AuthenticationConfiguration
import com.ably.tracking.connection.ConnectionConfigurationKey
import com.ably.tracking.connection.ConnectionConfigurationToken
import com.ably.tracking.connection.TokenRequest
import com.ably.tracking.connection.TokenRequestParameters

/**
 * Static factory used to create supported [AuthenticationConfiguration] from Java code.
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
