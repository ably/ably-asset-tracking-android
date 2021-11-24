package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias AsyncWork<T> = (suspend () -> T)

sealed class WorkResult

data class SyncAsyncResult(val syncWorkResult: WorkResult? = null, val asyncWork: AsyncWork<WorkResult>? = null)
    :WorkResult()

sealed class AddTrackableWorkResult() : WorkResult() {
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

