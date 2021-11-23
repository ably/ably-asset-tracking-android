package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.resulthandlers.AddTrackableResultHandler
import com.ably.tracking.publisher.eventqueue.workers.Worker
import kotlinx.coroutines.flow.StateFlow

internal interface WorkResultHandler {
    //core publisher is here temporarily so we can  stay bridged with existing architecture.
    fun handle(
        workResult: WorkResult, resultCallbackFunctions: ResultCallbackFunction<StateFlow<TrackableState>>?, corePublisher:
        CorePublisher
    )
        : WorkResultHandlerResult?
}

// a class that represent the result from work result handler so that the caller might use wrappeed objects to
// enqueue again
data class WorkResultHandlerResult(
    val worker: Worker, val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>? =
        null
)

//Get handler for work result
internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler {
    when (workResult) {
        is AddTrackableWorkResult -> AddTrackableResultHandler()
    }
    throw IllegalArgumentException()
}

