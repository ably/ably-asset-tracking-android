package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.AddTrackableFailedWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class AddTrackableFailedResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<AddTrackableFailedWorkResult> {
    override fun handle(workResult: AddTrackableFailedWorkResult): Worker? =
        when (workResult) {
            AddTrackableFailedWorkResult.StopConnectionCompleted ->
                workerFactory.createWorker(WorkerParams.StoppingConnectionFinished)
        }
}
