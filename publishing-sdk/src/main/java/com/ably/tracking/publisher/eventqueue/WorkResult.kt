package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow

typealias AsyncWork<T> = (suspend () -> T)

sealed class WorkResult

data class SyncAsyncResult(val syncWorkResult: WorkResult? = null, val asyncWork: AsyncWork<WorkResult>? = null)
    :WorkResult()

sealed class AddTrackableResult() : WorkResult() {
    data class AlreadyIn(val trackable: MutableStateFlow<TrackableState>) : AddTrackableResult()
    data class Success(val trackable: Trackable) : AddTrackableResult()
    data class Fail(val trackable: Trackable, val exception: Throwable?) :
        AddTrackableResult()
}

