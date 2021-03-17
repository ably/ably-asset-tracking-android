package com.ably.tracking

sealed class ConnectionConfiguration(val clientId: String)

/**
 *  Represents a [ConnectionConfiguration] that uses the basic authentication and requires to provide the API key for Ably.
 */
class ConnectionConfigurationKey
private constructor(val apiKey: String, clientId: String) : ConnectionConfiguration(clientId) {
    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun create(apiKey: String, clientId: String) = ConnectionConfigurationKey(apiKey, clientId)
    }

    private constructor() : this("", "")
}

/**
 *  Represents a [ConnectionConfiguration] that uses the token authentication and requires to provide a callback that will be called each time a new token or token request is required.
 */
class ConnectionConfigurationToken
private constructor(val callback: (TokenRequestParameters) -> Any, clientId: String) :
    ConnectionConfiguration(clientId) {
    companion object {
        /**
         * @param callback Callback that will be called with [TokenRequestParameters] each time a token or token request needs to be obtained or renewed.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun create(callback: (TokenRequestParameters) -> Any, clientId: String) =
            ConnectionConfigurationToken(callback, clientId)

        /**
         * @param callback [TokenRequestCallback] that will be called each time a token or token request needs to be obtained or renewed.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun create(callback: TokenRequestCallback, clientId: String) =
            ConnectionConfigurationToken(callback, clientId)
    }

    private constructor() : this({}, "")
    private constructor(callback: TokenRequestCallback, clientId: String) : this(
        { callback.onRequestToken(it) },
        clientId
    )
}

/**
 * Interface provided for those using the [ConnectionConfigurationToken] from Java code.
 */
interface TokenRequestCallback {
    /**
     * This method will be called each time a token or token request needs to be obtained or renewed.
     *
     * @param requestParameters Parameters of the token request
     * @return signed TokenRequest, TokenDetails or a token string
     */
    fun onRequestToken(requestParameters: TokenRequestParameters): Any
}

/**
 * Represents a set of parameters that are used when requesting an auth token.
 */
data class TokenRequestParameters(
    /**
     * Time to live of the token.
     * If set to 0 then the default value will be used.
     */
    val ttl: Long,
    /**
     * Capabilities of the token.
     */
    val capability: String,
    /**
     * Client ID associated with the token.
     */
    val clientId: String,
    /**
     * Timestamp in milliseconds of this request.
     */
    val timestamp: Long
)
