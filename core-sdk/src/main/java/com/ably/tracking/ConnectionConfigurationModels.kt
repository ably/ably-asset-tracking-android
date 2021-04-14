package com.ably.tracking

/**
 *  Base interface for connection configurations.
 */
interface ConnectionConfiguration {
    val clientId: String
}

/**
 *  Represents a [ConnectionConfiguration] that uses the basic authentication and requires to provide the API key for Ably.
 */
interface ConnectionConfigurationKey : ConnectionConfiguration {
    /**
     * Ably key string as obtained from the dashboard.
     **/
    val apiKey: String

    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun create(apiKey: String, clientId: String): ConnectionConfigurationKey =
            DefaultConfiguration(apiKey, clientId)
    }

    private class DefaultConfiguration(
        override val apiKey: String,
        override val clientId: String
    ) : ConnectionConfigurationKey
}

/**
 *  Represents a [ConnectionConfiguration] that uses the token authentication and requires to provide a callback that will be called each time a new [TokenRequest] is required.
 */
interface ConnectionConfigurationToken : ConnectionConfiguration {
    /**
     * Callback that will be called with [TokenRequestParameters] each time a [TokenRequest] needs to be obtained.
     **/
    val callback: (TokenRequestParameters) -> TokenRequest

    companion object {
        /**
         * @param callback Callback that will be called with [TokenRequestParameters] each time a [TokenRequest] needs to be obtained.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun create(callback: (TokenRequestParameters) -> TokenRequest, clientId: String): ConnectionConfigurationToken =
            DefaultConfiguration(callback, clientId)
    }

    private class DefaultConfiguration(
        override val callback: (TokenRequestParameters) -> TokenRequest,
        override val clientId: String
    ) : ConnectionConfigurationToken
}

/**
 * Represents a set of parameters that are passed to [TokenRequestCallback.onRequestToken] when Ably SDK is requesting an auth token.
 */
open class TokenRequestParameters(
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

/**
 * Represents a signed token request that should be created by the SDK user. It is used by Ably to get the authentication token.
 * More info available [here](https://ably.com/documentation/core-features/authentication#token-request-process).
 */
class TokenRequest(
    /**
     * Time to live of the token.
     * If set to 0 then the default value will be used.
     */
    ttl: Long,

    /**
     * Capabilities of the token.
     */
    capability: String,

    /**
     * Client ID associated with the token.
     */
    clientId: String,

    /**
     * Timestamp in milliseconds of this request.
     */
    timestamp: Long,

    /**
     * The keyName of the key against which this request is made.
     */
    var keyName: String,

    /**
     * An opaque nonce string of at least 16 characters to ensure uniqueness of this request. Any subsequent request using the same nonce will be rejected.
     */
    var nonce: String,

    /**
     * The Message Authentication Code for this request.
     */
    var mac: String
) : TokenRequestParameters(ttl, capability, clientId, timestamp)
