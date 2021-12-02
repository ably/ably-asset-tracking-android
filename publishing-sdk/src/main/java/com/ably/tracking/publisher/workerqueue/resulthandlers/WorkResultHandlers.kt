package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.WorkResult
import com.ably.tracking.publisher.workerqueue.WorkResultHandler

internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler {
    when (workResult) {
        is AddTrackableWorkResult -> return AddTrackableResultHandler()
        is ConnectionCreatedWorkResult -> return ConnectionCreatedResultHandler()
        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
}
