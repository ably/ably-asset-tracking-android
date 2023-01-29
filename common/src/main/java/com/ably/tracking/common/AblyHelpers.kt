package com.ably.tracking.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.TokenAuthException
import com.ably.tracking.TokenAuthNonRetriableException
import com.ably.tracking.CouldNotFetchTokenException
import com.ably.tracking.common.message.PresenceDataMessage
import com.ably.tracking.common.message.toTracking
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.TokenParams
import com.ably.tracking.connection.TokenRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ably.lib.realtime.ChannelState
import io.ably.lib.rest.Auth
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.runBlocking

/**
 * Extension converting Ably Realtime connection state to the equivalent [ConnectionState] API presented to users of
 * the Ably Asset Tracking SDKs.
 */
fun io.ably.lib.realtime.ConnectionState.toTracking(): ConnectionState = when (this) {
    io.ably.lib.realtime.ConnectionState.initialized -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.connecting -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.connected -> ConnectionState.ONLINE
    io.ably.lib.realtime.ConnectionState.disconnected -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.suspended -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.closing -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.closed -> ConnectionState.OFFLINE
    io.ably.lib.realtime.ConnectionState.failed -> ConnectionState.FAILED
}

/**
 * Extension converting Ably error info objects to the equivalent [ErrorInformation] API presented to users of the Ably
 * Asset Tracking SDKs.
 *
 * The `requestId` field is yet to be implemented by ably-java, however even once it is available then the chances are
 * that it'll still not be exposed through to users of the Ably Asset Tracking SDKs in order to keep things simple.
 */
fun io.ably.lib.types.ErrorInfo.toTracking() =
    ErrorInformation(
        this.code,
        this.statusCode,
        this.message,
        this.href, // may be null
        null // yet to be implemented by ably-java
    )

/**
 * Extension converting Ably error info objects to the equivalent [ConnectionException] API presented to users of the Ably
 * Asset Tracking SDKs.
 *
 * The `requestId` field is yet to be implemented by ably-java, however even once it is available then the chances are
 * that it'll still not be exposed through to users of the Ably Asset Tracking SDKs in order to keep things simple.
 */
fun io.ably.lib.types.ErrorInfo.toTrackingException() =
    ConnectionException(
        ErrorInformation(
            this.code,
            this.statusCode,
            this.message,
            this.href, // may be null
            null // yet to be implemented by ably-java
        )
    )

/**
 * Extension converting Ably Realtime connection state change events to the equivalent [ConnectionStateChange] API
 * presented to users of the Ably Asset Tracking SDKs.
 *
 * We are intentionally not passing on the `event` or `retryIn` fields. Our position on this may change in future, but
 * they are omitted for now to keep things simple.
 */
fun io.ably.lib.realtime.ConnectionStateListener.ConnectionStateChange.toTracking() =
    ConnectionStateChange(
        this.current.toTracking(),
        this.reason?.toTracking()
    )

/**
 * Extension vending Ably client library ClientOptions from an [Authentication] instance.
 */
val Authentication.clientOptions: ClientOptions
    get() = ClientOptions().apply {
        clientId = this@clientOptions.clientId

        this@clientOptions.tokenRequestConfiguration?.callback?.let { tokenRequestCallback ->
            authCallback = Auth.TokenCallback {
                runBlocking {
                    try {
                        tokenRequestCallback(it.toTracking()).toAuth()
                    } catch (exception: TokenAuthException) {
                        throw exception.toAblyException()
                    }
                }
            }
        }

        this@clientOptions.tokenRequestConfiguration?.staticTokenRequest?.let { staticTokenRequest ->
            authCallback = Auth.TokenCallback { staticTokenRequest.toAuth() }
        }

        this@clientOptions.jwtConfiguration?.callback?.let { jwtCallback ->
            authCallback = Auth.TokenCallback {
                runBlocking {
                    try {
                        jwtCallback(it.toTracking())
                    } catch (exception: TokenAuthException) {
                        throw exception.toAblyException()
                    }
                }
            }
        }

        this@clientOptions.jwtConfiguration?.staticJwt?.let { staticJwt ->
            authCallback = Auth.TokenCallback { staticJwt }
        }

        this@clientOptions.basicApiKey?.let { basicApiKey ->
            key = basicApiKey
        }
    }

/**
 * Extension converting Ably Realtime auth token params to the equivalent [TokenParams] API
 * presented to users of the Ably Asset Tracking SDKs.
 */
fun Auth.TokenParams.toTracking(): TokenParams =
    object : TokenParams {
        override val ttl: Long = this@toTracking.ttl
        override val capability: String? = this@toTracking.capability
        override val clientId: String? = this@toTracking.clientId
        override val timestamp: Long = this@toTracking.timestamp
    }

/**
 * Extension converting Asset Tracking SDK [TokenRequest] to the equivalent [Auth.TokenRequest] from Ably.
 */
fun TokenRequest.toAuth(): Auth.TokenRequest =
    Auth.TokenRequest().apply {
        ttl = this@toAuth.ttl
        capability = this@toAuth.capability
        clientId = this@toAuth.clientId
        timestamp = this@toAuth.timestamp
        keyName = this@toAuth.keyName
        nonce = this@toAuth.nonce
        mac = this@toAuth.mac
    }

/**
 * Extension converting Ably Realtime presence message to the equivalent [PresenceMessage] API
 * presented to users of the Ably Asset Tracking SDKs.
 * If presence data is missing or in wrong format it returns null.
 */
fun io.ably.lib.types.PresenceMessage.toTracking(gson: Gson): PresenceMessage? =
    this.getPresenceData(gson)?.let { presenceData ->
        PresenceMessage(
            this.action.toTracking(),
            presenceData,
            "${this.connectionId}:${this.clientId}"
        )
    }

fun io.ably.lib.types.PresenceMessage.Action.toTracking(): PresenceAction = when (this) {
    io.ably.lib.types.PresenceMessage.Action.present -> PresenceAction.PRESENT_OR_ENTER
    io.ably.lib.types.PresenceMessage.Action.enter -> PresenceAction.PRESENT_OR_ENTER
    io.ably.lib.types.PresenceMessage.Action.update -> PresenceAction.UPDATE
    io.ably.lib.types.PresenceMessage.Action.leave -> PresenceAction.LEAVE_OR_ABSENT
    io.ably.lib.types.PresenceMessage.Action.absent -> PresenceAction.LEAVE_OR_ABSENT
}

/**
 * Extension converting Ably Realtime channel state to the equivalent [ConnectionState] API presented to users of
 * the Ably Asset Tracking SDKs.
 */
fun ChannelState.toTracking() = when (this) {
    ChannelState.initialized -> ConnectionState.OFFLINE
    ChannelState.attaching -> ConnectionState.OFFLINE
    ChannelState.attached -> ConnectionState.ONLINE
    ChannelState.detaching -> ConnectionState.OFFLINE
    ChannelState.detached -> ConnectionState.OFFLINE
    ChannelState.failed -> ConnectionState.FAILED
    ChannelState.suspended -> ConnectionState.OFFLINE
}

/**
 * Extension converting Ably Realtime channel state change events to the equivalent [ConnectionStateChange] API
 * presented to users of the Ably Asset Tracking SDKs.
 */
fun AblySdkChannelStateListener.ChannelStateChange.toTracking() =
    ConnectionStateChange(
        this.current.toTracking(),
        this.reason?.toTracking()
    )

/**
 * Returns parsed data or null if data is missing or in wrong format.
 *
 * We expect the data to be in one of two formats: String or JsonObject.
 * String data is being sent from AAT Android and AAT Swift.
 * JsonObject data is being sent from AAT JavaScript.
 */
fun io.ably.lib.types.PresenceMessage.getPresenceData(gson: Gson): PresenceData? =
    when (data) {
        is String -> gson.fromJsonOrNull(data as? String, PresenceDataMessage::class.java)?.toTracking()
        is JsonObject -> gson.fromJsonOrNull(data.toString(), PresenceDataMessage::class.java)?.toTracking()
        else -> null
    }

/**
 * Maps a [TokenAuthException] to a corresponding [AblyException] that can influence Ably SDK auth flow.
 */
private fun TokenAuthException.toAblyException(): AblyException =
    when (this) {
        is TokenAuthNonRetriableException ->
            AblyException.fromErrorInfo(ErrorInfo(message, 403, 100_003))
        is CouldNotFetchTokenException ->
            AblyException.fromErrorInfo(ErrorInfo(message, 401, 100_002))
    }

/**
 * Indicates whether the result is a failure that a fatal exception from as a reason and we should not attempt to retry it.
 */
fun <T : Any> Result<T>.isFatalAblyFailure() =
    isFailure && (exceptionOrNull() as ConnectionException?)?.isFatal() == true

/**
 * Indicates whether the exception from Ably is fatal and we should not attempt to retry it.
 * Fatal errors have status codes like 4xx (e.g. 400).
 */
fun ConnectionException.isFatal(): Boolean {
    return (400..499).contains(errorInformation.statusCode)
}

/**
 * Indicates whether the exception from Ably is retriable and we can attempt to retry it.
 * Non-fatal errors have status codes in range 500-504.
 */
fun ConnectionException.isRetriable(): Boolean {
    return (500..504).contains(errorInformation.statusCode)
}
