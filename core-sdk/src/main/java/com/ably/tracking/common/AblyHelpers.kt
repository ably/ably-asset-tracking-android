package com.ably.tracking

/**
 * Extension converting Ably Realtime connection state to the equivalent [ConnectionState] API presented to users of
 * the Ably Asset Tracking SDKs.
 */
fun io.ably.lib.realtime.ConnectionState.toTracking() = when (this) {
    io.ably.lib.realtime.ConnectionState.initialized -> ConnectionState.INITIALIZED
    io.ably.lib.realtime.ConnectionState.connecting -> ConnectionState.CONNECTING
    io.ably.lib.realtime.ConnectionState.connected -> ConnectionState.CONNECTED
    io.ably.lib.realtime.ConnectionState.disconnected -> ConnectionState.DISCONNECTED
    io.ably.lib.realtime.ConnectionState.suspended -> ConnectionState.SUSPENDED
    io.ably.lib.realtime.ConnectionState.closing -> ConnectionState.CLOSING
    io.ably.lib.realtime.ConnectionState.closed -> ConnectionState.CLOSED
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
 * Extension converting Ably Realtime connection state change events to the equivalent [ConnectionStateChange] API
 * presented to users of the Ably Asset Tracking SDKs.
 *
 * We are intentionally not passing on the `event` or `retryIn` fields. Our position on this may change in future, but
 * they are omitted for now to keep things simple.
 */
fun io.ably.lib.realtime.ConnectionStateListener.ConnectionStateChange.toTracking() =
    ConnectionStateChange(
        this.current.toTracking(),
        this.previous.toTracking(),
        this.reason?.toTracking()
    )
