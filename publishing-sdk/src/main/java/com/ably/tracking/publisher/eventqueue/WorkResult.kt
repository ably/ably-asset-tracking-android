package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.publisher.AddTrackableHandler
import com.ably.tracking.publisher.Trackable

sealed class WorkResult
sealed class AddTrackableResult : WorkResult() {
    data class Success(val trackable: Trackable, val handler: AddTrackableHandler) : AddTrackableResult()
    data class Fail(val trackable: Trackable, val handler: AddTrackableHandler, val exception: Throwable?) :
        AddTrackableResult()
}

