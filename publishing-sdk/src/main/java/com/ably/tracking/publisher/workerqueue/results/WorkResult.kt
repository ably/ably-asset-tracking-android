package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias AsyncWork<T> = (suspend () -> T)

internal sealed class WorkResult

/**
 * A special [WorkResult] that contains an optional [syncWorkResult] and [asyncWork]
 * [syncWorkResult] is any work result that resulted from sync work. Caller is responsible for how it should handle
 * this result
 * [asyncWork] is any work that is not immediately executed but returned to caller for it to execute it. It is a
 * suspending work, so it's intended to be used inside a coroutine scope.
 * **/
internal data class SyncAsyncResult(val syncWorkResult: WorkResult? = null, val asyncWork: AsyncWork<WorkResult>? = null)
    :WorkResult()

internal sealed class AddTrackableWorkResult() : WorkResult() {
    data class AlreadyIn(
        val trackableStateFlow: MutableStateFlow<TrackableState>,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()
    data class Success(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()
    data class Fail(
        val trackable: Trackable,
        val exception: Throwable?,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) :
        AddTrackableWorkResult()
}

