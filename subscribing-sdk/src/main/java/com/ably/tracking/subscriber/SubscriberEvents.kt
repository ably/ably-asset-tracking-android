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
 * Represents an event that invokes an action that calls a callback when it completes.
 */
internal sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()

internal class StartEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class ConnectionCreatedEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class ConnectionReadyEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class StopEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : AdhocEvent()

internal class ChangeResolutionEvent(
    val resolution: Resolution?,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal data class AblyConnectionStateChangeEvent(val connectionStateChange: ConnectionStateChange) : AdhocEvent()

internal data class ChannelConnectionStateChangeEvent(val connectionStateChange: ConnectionStateChange) : AdhocEvent()
