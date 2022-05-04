package com.ably.tracking.connection

data class ConnectionConfiguration(val authentication: Authentication)

typealias TokenRequestCallback = (TokenParams) -> TokenRequest
typealias JwtCallback = (TokenParams) -> String

sealed class Authentication(
    val clientId: String,
    val basicApiKey: String?,
    val tokenRequestCallback: TokenRequestCallback?,
    val jwtCallback: JwtCallback?
) {
    init {
        if (tokenRequestCallback != null && basicApiKey != null && jwtCallback != null) {
            // This indicates a mistake in the implementation of the Authentication class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("Multiple authentication methods.")
        }

        if (tokenRequestCallback == null && basicApiKey == null && jwtCallback == null) {
            // This indicates a mistake in the implementation of the Authentication class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("No authentication methods.")
        }
    }

    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun basic(clientId: String, apiKey: String): Authentication =
            BasicAuthentication(clientId, apiKey)

        /**
         * @param callback Callback that will be called with [TokenParams] each time a [TokenRequest] needs to be obtained.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun tokenRequest(clientId: String, callback: TokenRequestCallback): Authentication =
            TokenAuthentication(clientId, callback)

        /**
         * @param callback Callback that will be called with [TokenParams] each time a JWT string needs to be obtained.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun jwt(clientId: String, callback: JwtCallback): Authentication =
            JwtAuthentication(clientId, callback)
    }
}

private class BasicAuthentication(clientId: String, apiKey: String) :
    Authentication(clientId, apiKey, null, null)

private class TokenAuthentication(clientId: String, callback: TokenRequestCallback) :
    Authentication(clientId, null, callback, null)

private class JwtAuthentication(clientId: String, callback: JwtCallback) :
    Authentication(clientId, null, null, callback)

interface TokenParams {
    /**
     * Time to live of the token.
     * If set to 0 then the default value will be used.
     */
    val ttl: Long

    /**
     * Capabilities of the token.
     */
    val capability: String?

    /**
     * Client ID associated with the token.
     */
    val clientId: String

    /**
     * Timestamp in milliseconds of this request.
     */
    val timestamp: Long
}

/**
 * Represents a signed token request that should be created by the SDK user. It is used by Ably to get the authentication token.
 * More info available [here](https://ably.com/docs/core-features/authentication#token-request-process).
 */
interface TokenRequest : TokenParams {
    /**
     * The keyName of the key against which this request is made.
     */
    val keyName: String

    /**
     * An opaque nonce string of at least 16 characters to ensure uniqueness of this request. Any subsequent request using the same nonce will be rejected.
     */
    val nonce: String

    /**
     * The Message Authentication Code for this request.
     */
    val mac: String
}
