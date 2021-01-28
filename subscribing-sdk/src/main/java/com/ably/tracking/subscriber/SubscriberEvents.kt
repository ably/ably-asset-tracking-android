package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import io.ably.lib.types.PresenceMessage

internal sealed class Event

// TODO - add docs (adhoc represents an event without a callback)
internal sealed class AdhocEvent : Event()

// TODO - add docs (request represents an event with a callback)
internal sealed class Request : Event()

internal class StartEvent : AdhocEvent()

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
