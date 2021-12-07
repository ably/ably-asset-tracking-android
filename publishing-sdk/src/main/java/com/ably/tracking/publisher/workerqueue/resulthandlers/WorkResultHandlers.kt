package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> getWorkResultHandler(workResult: T): WorkResultHandler<WorkResult> {
    when (workResult) {
        AddTrackableWorkResult::class -> return AddTrackableResultHandler() as WorkResultHandler<WorkResult>
        ConnectionCreatedWorkResult::class -> return ConnectionCreatedResultHandler() as WorkResultHandler<WorkResult>
        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
}
