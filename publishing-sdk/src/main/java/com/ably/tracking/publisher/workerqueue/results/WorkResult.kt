package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias AsyncWork<T> = (suspend () -> T)

/**
 * This sealed class represents a base result from concrete  [Worker] instances after they finish doing their work.
 * It's also intended to be returned by [AsyncWork] coroutines.
 * **/
internal sealed class WorkResult

/**
 * A special [WorkResult] that contains an optional [syncWorkResult] and [asyncWork]
 * [syncWorkResult] : Any work result that has resulted from synchronous work. This work result is expected to be
 * returned and processed in a blocking fashion
 * [asyncWork] is any work that is not immediately executed but returned to caller for it to execute later. It is a
 * suspending work and it is intended to be launched in a different coroutine. However there is nothing that prevents
 * caller from executing this in the same coroutine. If any use case like this arise, you should document the reason
 * of such usage.
 * **/
internal data class SyncAsyncResult(
    val syncWorkResult: WorkResult? = null,
    val asyncWork: AsyncWork<WorkResult>? = null
) : WorkResult()

internal sealed class AddTrackableWorkResult() : WorkResult() {
    internal data class AlreadyIn(
        val trackableStateFlow: MutableStateFlow<TrackableState>,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class Success(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class Fail(
        val trackable: Trackable,
        val exception: Throwable?,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()
}
