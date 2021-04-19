package com.ably.tracking.java

import com.ably.tracking.connection.BasicAuthenticationConfiguration
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.connection.TokenAuthenticationConfiguration
import com.ably.tracking.connection.TokenRequest
import com.ably.tracking.connection.TokenParams

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
        fun createBasic(apiKey: String, clientId: String) =
            ConnectionConfiguration(BasicAuthenticationConfiguration.create(apiKey, clientId))

        /**
         * @param callback [TokenRequestCallback] that will be called each time a [TokenRequest] needs to be obtained.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun createToken(callback: TokenRequestCallback, clientId: String) =
            ConnectionConfiguration(TokenAuthenticationConfiguration.create({ callback.onRequestToken(it) }, clientId))
    }
}

/**
 * Interface provided for those using the [TokenAuthenticationConfiguration] from Java code.
 */
interface TokenRequestCallback {
    /**
     * This method will be called each time a [TokenRequest] needs to be obtained.
     *
     * @param tokenParams Parameters of the token request
     * @return [TokenRequest] used to obtain the token
     */
    fun onRequestToken(tokenParams: TokenParams): TokenRequest
}
