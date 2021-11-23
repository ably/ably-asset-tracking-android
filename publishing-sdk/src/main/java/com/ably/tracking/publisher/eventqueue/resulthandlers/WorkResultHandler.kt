package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.resulthandlers.AddTrackableResultHandler
import com.ably.tracking.publisher.eventqueue.workers.Worker

internal interface WorkResultHandler<in T : WorkResult> {
    //core publisher is here temporarily so we can  stay bridged with existing architecture.
    fun handle(workResult: T, resultCallbackFunctions: List<ResultCallbackFunction<*>>?, corePublisher: CorePublisher)
        : WorkResultHandlerResult?
}

// a class that represent the result from work result handler so that the caller might use wrappeed objects to
// enqueue again
data class WorkResultHandlerResult(
    val worker: Worker, val resultCallbackFunctions: List<ResultCallbackFunction<*>>? =
        null
)

//Get handler as generic interface
@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> getWorkResultHandler(workResult: T): WorkResultHandler<WorkResult> {
    when (workResult) {
        AddTrackableResult::class -> return AddTrackableResultHandler() as WorkResultHandler<WorkResult>
    }
    throw IllegalArgumentException()
}

