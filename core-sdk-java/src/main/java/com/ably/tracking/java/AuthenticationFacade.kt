package com.ably.tracking.java

import com.ably.tracking.TokenAuthException
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.TokenParams
import com.ably.tracking.connection.TokenRequest
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

interface TokenRequestCreator {
    /**
     * This method will be called with [TokenParams] each time a [TokenRequest] needs to be obtained.
     * If something goes wrong while fetching the token you should throw a [TokenAuthException] from the method or the resulting [CompletableFuture].
     */
    @Throws(TokenAuthException::class)
    fun onCreateNewToken(tokenParams: TokenParams): CompletableFuture<TokenRequest>
}

interface JwtCreator {
    /**
     * This method will be called with [TokenParams] each time a JWT string needs to be obtained.
     * If something goes wrong while fetching the token you should throw a [TokenAuthException] from the method or the resulting [CompletableFuture].
     */
    @Throws(TokenAuthException::class)
    fun onCreateNewToken(tokenParams: TokenParams): CompletableFuture<String>
}

class AuthenticationFacade {
    companion object {
        /**
         * @param apiKey Ably key string as obtained from the dashboard.
         * @param clientId ID of the client
         */
        @JvmStatic
        fun basic(clientId: String, apiKey: String): Authentication =
            Authentication.basic(clientId, apiKey)

        /**
         * Authentication method that uses the Token Request. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a [TokenRequest] needs to be obtained.
         */
        @JvmStatic
        fun tokenRequest(callback: TokenRequestCreator): Authentication =
            Authentication.tokenRequest {
                callback.onCreateNewToken(it).await()
            }

        /**
         * Authentication method that uses the JWT. The [callback] will be called each time a new token is needed.
         * If something goes wrong while fetching the token you should throw a [TokenAuthException] in the [callback].
         *
         * @param callback Callback that will be called with [TokenParams] each time a JWT string needs to be obtained.
         */
        @JvmStatic
        fun jwt(callback: JwtCreator): Authentication =
            Authentication.jwt {
                callback.onCreateNewToken(it).await()
            }
    }
}
