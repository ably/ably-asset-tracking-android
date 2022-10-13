package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.DisconnectSuccessWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class DisconnectSuccessResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<DisconnectSuccessWorkResult> {
    override fun handle(workResult: DisconnectSuccessWorkResult): Worker? =
        when (workResult) {
            DisconnectSuccessWorkResult.StopConnectionCompleted ->
                workerFactory.createWorker(WorkerParams.StoppingConnectionFinished)
        }
}
