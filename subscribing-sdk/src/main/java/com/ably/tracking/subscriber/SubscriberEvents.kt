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
 * Represents an event that invokes an action that calls a callback when it completes.
 */
internal sealed class Request<T>(val handler: ResultHandler<T>) : Event()

internal class StartEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal class StopEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : AdhocEvent()

internal class ChangeResolutionEvent(
    val resolution: Resolution?,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal data class AblyConnectionStateChangeEvent(val connectionStateChange: ConnectionStateChange) : AdhocEvent()

internal data class ChannelConnectionStateChangeEvent(val connectionStateChange: ConnectionStateChange) : AdhocEvent()
