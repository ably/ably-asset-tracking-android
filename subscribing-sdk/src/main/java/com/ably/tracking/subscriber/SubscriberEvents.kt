package com.ably.tracking.subscriber

import com.ably.tracking.common.ResultCallbackFunction

internal sealed class Event

/**
 * Represents an event that invokes an action that calls the [callbackFunction] when it completes.
 */
internal sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()
