package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction

internal sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
internal sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls the [handler] when it completes.
 */
internal sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()

/**
 * Start the [CoreSubscriber].
 */
internal class StartEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Successfully created a connection for a trackable.
 * Should be created only from within the [CoreSubscriber].
 */
internal class ConnectionCreatedEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Connection for a trackable is ready to be used.
 * Should be created only from within the [CoreSubscriber].
 */
internal class ConnectionReadyEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Stop the [CoreSubscriber].
 */
internal class StopEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * A new presence message is received.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : AdhocEvent()

/**
 * Change the requested [Resolution].
 */
internal class ChangeResolutionEvent(
    val resolution: Resolution?,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Ably connection state changed.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class AblyConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()

/**
 * Trackable Ably channel state changed.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class ChannelConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()
