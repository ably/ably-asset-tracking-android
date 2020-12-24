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
 * Extension converting Ably Realtime connection state change events to the equivalent [ConnectionStateChange] API
 * presented to users of the Ably Asset Tracking SDKs.
 */
fun io.ably.lib.realtime.ConnectionStateListener.ConnectionStateChange.toTracking() =
    ConnectionStateChange(this.previous.toTracking(), this.current.toTracking())
