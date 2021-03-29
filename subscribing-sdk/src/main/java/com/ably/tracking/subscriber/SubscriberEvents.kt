package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.PresenceMessage

internal sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
internal sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls a callback when it completes.
 */
internal sealed class Request : Event()

internal class StartEvent(
    val handler: ResultHandler<Unit>
) : Request()

internal class StopEvent(
    val handler: ResultHandler<Unit>
) : Request()

internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : AdhocEvent()

internal data class ChangeResolutionEvent(
    val resolution: Resolution?,
    val handler: ResultHandler<Unit>
) : Request()
