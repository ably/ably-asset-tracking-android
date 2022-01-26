package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult

@Suppress("UNCHECKED_CAST")
internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler<WorkResult> =
    when (workResult) {
        is AddTrackableWorkResult -> AddTrackableResultHandler() as WorkResultHandler<WorkResult>
        is ConnectionCreatedWorkResult -> ConnectionCreatedResultHandler() as WorkResultHandler<WorkResult>
        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
