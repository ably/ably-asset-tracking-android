package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultHandler

internal sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
internal sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls the [handler] when it completes.
 */
internal sealed class Request<T>(val handler: ResultHandler<T>) : Event()

/**
 * Event created when wanting to start the [CoreSubscriber].
 */
internal class StartEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when a connection for a trackable is successfully created.
 * Should be created only from within the [CoreSubscriber].
 */
internal class ConnectionCreatedEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when a connection for a trackable is ready to be used.
 * Should be created only from within the [CoreSubscriber].
 */
internal class ConnectionReadyEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when wanting to stop the [CoreSubscriber]
 */
internal class StopEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created each time when a presence message is received.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : AdhocEvent()

/**
 * Event created when wanting to change the requested [Resolution].
 */
internal class ChangeResolutionEvent(
    val resolution: Resolution?,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created each time the Ably connection state changes.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class AblyConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()

/**
 * Event created each time the trackable Ably channel state changes.
 * Should be created only from within the [CoreSubscriber].
 */
internal data class ChannelConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()
