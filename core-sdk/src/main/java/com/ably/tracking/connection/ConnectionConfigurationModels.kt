package com.ably.tracking.connection

data class ConnectionConfiguration(val authentication: Authentication)

typealias TokenRequestCallback = (TokenParams) -> TokenRequest

sealed class Authentication(val clientId: String) {
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
        @JvmStatic
        fun tokenRequest(clientId: String, callback: TokenRequestCallback): Authentication =
            TokenAuthentication(clientId, callback)
    }

    abstract val basicApiKey: String?
    abstract val tokenRequestCallback: TokenRequestCallback?
}

private class BasicAuthentication
constructor(clientId: String, val apiKey: String) :
    Authentication(clientId) {
    override val basicApiKey: String?
        get() = apiKey
    override val tokenRequestCallback: TokenRequestCallback?
        get() = null
}

private class TokenAuthentication
constructor(clientId: String, val callback: TokenRequestCallback) :
    Authentication(clientId) {
    override val basicApiKey: String?
        get() = null
    override val tokenRequestCallback: TokenRequestCallback?
        get() = callback
}

interface TokenParams {
    /**
     * Time to live of the token.
     * If set to 0 then the default value will be used.
     */
    val ttl: Long

    /**
     * Capabilities of the token.
     */
    val capability: String

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
 * More info available [here](https://ably.com/documentation/core-features/authentication#token-request-process).
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
