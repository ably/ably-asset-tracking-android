package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.SetActiveTrackableResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult

internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler {
    when (workResult) {
        is AddTrackableWorkResult -> return AddTrackableResultHandler()
        is ConnectionCreatedWorkResult -> return ConnectionCreatedResultHandler()
        is SetActiveTrackableResult -> return SetActiveTrackableResultHandler()
        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
}
