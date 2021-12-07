package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> getWorkResultHandler(workResult: T): WorkResultHandler<WorkResult> {
    return when (workResult!!::class) {
        AddTrackableWorkResult.Success::class, AddTrackableWorkResult.Fail::class, AddTrackableWorkResult.AlreadyIn::class
        -> AddTrackableResultHandler() as WorkResultHandler<WorkResult>

        ConnectionCreatedWorkResult.PresenceSuccess::class, ConnectionCreatedWorkResult.PresenceFail::class,
        ConnectionCreatedWorkResult.RemovalRequested::class -> ConnectionCreatedResultHandler() as WorkResultHandler<WorkResult>

        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
}
