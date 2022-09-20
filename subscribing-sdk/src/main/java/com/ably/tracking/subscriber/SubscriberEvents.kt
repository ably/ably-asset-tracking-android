package com.ably.tracking.subscriber

import com.ably.tracking.Resolution
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction

internal sealed class Event

/**
 * Represents an event that invokes an action that calls the [callbackFunction] when it completes.
 */
internal sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()

/**
 * Change the requested [Resolution].
 */
internal class ChangeResolutionEvent(
    val resolution: Resolution?,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)
