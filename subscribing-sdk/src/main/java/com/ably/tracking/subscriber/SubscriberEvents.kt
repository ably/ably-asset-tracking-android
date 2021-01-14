package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import io.ably.lib.types.PresenceMessage

internal sealed class Event

internal class StopEvent(
    val handler: ResultHandler<Unit>
) : Event()

internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : Event()

internal data class ChangeResolutionEvent(
    val resolution: Resolution?,
    val handler: ResultHandler<Unit>
) : Event()
