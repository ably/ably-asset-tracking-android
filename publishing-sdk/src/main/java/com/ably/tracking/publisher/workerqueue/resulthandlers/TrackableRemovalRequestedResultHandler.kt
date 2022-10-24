package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.TrackableRemovalRequestedWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class TrackableRemovalRequestedResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<TrackableRemovalRequestedWorkResult> {
    override fun handle(workResult: TrackableRemovalRequestedWorkResult): Worker? =
        when (workResult) {
            TrackableRemovalRequestedWorkResult.StopConnectionCompleted ->
                workerFactory.createWorker(WorkerParams.StoppingConnectionFinished)
        }
}
