package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.results.RetrySubscribeToPresenceWorkResult
import com.ably.tracking.publisher.workerqueue.results.TrackableRemovalRequestedWorkResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult

@Suppress("UNCHECKED_CAST")
internal fun getWorkResultHandler(
    workResult: WorkResult,
    workerFactory: WorkerFactory,
): WorkResultHandler<WorkResult> =
    when (workResult) {
        is ConnectionCreatedWorkResult -> ConnectionCreatedResultHandler(workerFactory) as WorkResultHandler<WorkResult>
        is ConnectionReadyWorkResult -> ConnectionReadyResultHandler(workerFactory) as WorkResultHandler<WorkResult>
        is RetrySubscribeToPresenceWorkResult -> RetrySubscribeToPresenceResultHandler(workerFactory) as WorkResultHandler<WorkResult>
        is TrackableRemovalRequestedWorkResult -> TrackableRemovalRequestedResultHandler(workerFactory) as WorkResultHandler<WorkResult>
        else -> throw IllegalArgumentException("Invalid workResult provided")
    }
