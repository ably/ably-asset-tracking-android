package com.ably.tracking.connection

import com.ably.tracking.TokenAuthException

/**
 * Specifies the configuration for the Ably connection.
 */
data class ConnectionConfiguration(
    /**
     * The method of authentication; supported methods: basic, token request, jwt.
     */
    val authentication: Authentication,
    /**
     * For development or non-default production environments.
     * Allows a non-default Ably environment to be used such as 'sandbox'.
     */
    val environment: String? = null,
    /**
     * Specifies for how long should the SDK remain present in a channel when the connection is gone.
     * For more details please see the [Ably documentation](https://ably.com/docs/realtime/presence#unstable-connections).
     */
    val remainPresentForMilliseconds: Long? = null,
)

typealias TokenRequestCallback = suspend (TokenParams) -> TokenRequest
typealias JwtCallback = suspend (TokenParams) -> String

sealed class Authentication(
    val clientId: String?,
    val basicApiKey: String?,
    val tokenRequestConfiguration: TokenRequestConfiguration?,
    val jwtConfiguration: JwtConfiguration?
) {
    init {
        if (tokenRequestConfiguration != null && basicApiKey != null && jwtConfiguration != null) {
            // This indicates a mistake in the implementation of the Authentication class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("Multiple authentication methods.")
        }

        if (tokenRequestConfiguration == null && basicApiKey == null && jwtConfiguration == null) {
            // This indicates a mistake in the implementation of the Authentication class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("No authentication methods.")
        }

        if (basicApiKey != null && clientId == null) {
            // This indicates a mistake in the implementation of the Authentication class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("Basic authentication requires the client ID")
        }
    }

    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmSynthetic
        fun basic(clientId: String, apiKey: String): Authentication =
            BasicAuthentication(clientId, apiKey)

        /**
         * Authentication method that uses the Token Request. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a [TokenRequest] needs to be obtained.
         * @param clientId ID of the client
         */
        @Deprecated(
            message = "You should not need to provide the client ID when you are using a token-based auth",
            replaceWith = ReplaceWith("Authentication.tokenRequest(callback)"),
        )
        @JvmSynthetic
        fun tokenRequest(clientId: String, callback: TokenRequestCallback): Authentication =
            TokenAuthentication(clientId, TokenRequestConfiguration(callback, null))

        /**
         * Authentication method that uses the Token Request. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a [TokenRequest] needs to be obtained.
         */
        @JvmSynthetic
        fun tokenRequest(callback: TokenRequestCallback): Authentication =
            TokenAuthentication(null, TokenRequestConfiguration(callback, null))

        /**
         * Authentication method that uses the Token Request.
         * This is a convenience method that accepts a static token value that will be used each time the SDK needs to authenticate.
         *
         * @param staticTokenRequest The already created [TokenRequest] that will be used each time the SDK needs to authenticate.
         */
        @JvmSynthetic
        fun tokenRequest(staticTokenRequest: TokenRequest): Authentication =
            TokenAuthentication(null, TokenRequestConfiguration(null, staticTokenRequest))

        /**
         * Authentication method that uses the JWT. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a JWT string needs to be obtained.
         * @param clientId ID of the client
         */
        @Deprecated(
            message = "You should not need to provide the client ID when you are using a token-based auth",
            replaceWith = ReplaceWith("Authentication.jwt(callback)"),
        )
        @JvmSynthetic
        fun jwt(clientId: String, callback: JwtCallback): Authentication =
            JwtAuthentication(clientId, JwtConfiguration(callback, null))

        /**
         * Authentication method that uses the JWT. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a JWT string needs to be obtained.
         */
        @JvmSynthetic
        fun jwt(callback: JwtCallback): Authentication =
            JwtAuthentication(null, JwtConfiguration(callback, null))

        /**
         * Authentication method that uses the JWT.
         * This is a convenience method that accepts a static token value that will be used each time the SDK needs to authenticate.
         *
         * @param staticJwt The already created JWT that will be used each time the SDK needs to authenticate.
         */
        @JvmSynthetic
        fun jwt(staticJwt: String): Authentication =
            JwtAuthentication(null, JwtConfiguration(null, staticJwt))
    }
}

private class BasicAuthentication(clientId: String, apiKey: String) :
    Authentication(clientId, apiKey, null, null)

private class TokenAuthentication(clientId: String?, configuration: TokenRequestConfiguration) :
    Authentication(clientId, null, configuration, null)

private class JwtAuthentication(clientId: String?, configuration: JwtConfiguration) :
    Authentication(clientId, null, null, configuration)

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
    val clientId: String?

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

/**
 * Configuration object that is used to set up either the dynamic (callback-based) or static authentication.
 */
data class TokenRequestConfiguration(val callback: TokenRequestCallback?, val staticTokenRequest: TokenRequest?) {
    init {
        val isStaticEnabled = staticTokenRequest != null
        val isCallbackEnabled = callback != null
        if (!isCallbackEnabled && !isStaticEnabled) {
            // This indicates a mistake in the implementation of the TokenRequestConfiguration class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("Token request authentication requires either the callback or the static token request")
        }

        if (isCallbackEnabled && isStaticEnabled) {
            // This indicates a mistake in the implementation of the TokenRequestConfiguration class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("Token request authentication allows only one of the options: the callback or the static token request")
        }
    }
}

/**
 * Configuration object that is used to set up either the dynamic (callback-based) or static authentication.
 */
data class JwtConfiguration(val callback: JwtCallback?, val staticJwt: String?) {
    init {
        val isStaticEnabled = staticJwt != null
        val isCallbackEnabled = callback != null
        if (!isCallbackEnabled && !isStaticEnabled) {
            // This indicates a mistake in the implementation of the JwtConfiguration class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("JWT authentication requires either the callback or the static JWT")
        }

        if (isCallbackEnabled && isStaticEnabled) {
            // This indicates a mistake in the implementation of the JwtConfiguration class,
            // therefore not caused by the application (i.e. internal to this library).
            throw IllegalStateException("JWT authentication allows only one of the options: the callback or the static JWT")
        }
    }
}
