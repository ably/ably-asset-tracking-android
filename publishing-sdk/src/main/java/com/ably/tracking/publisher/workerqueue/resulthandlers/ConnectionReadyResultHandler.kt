package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionReadyResultHandler : WorkResultHandler<ConnectionReadyWorkResult> {
    override fun handle(
        workResult: ConnectionReadyWorkResult,
        corePublisher: CorePublisher,
        workerFactory: WorkerFactory
    ): Worker? {
        when (workResult) {
            is ConnectionReadyWorkResult.RemovalRequested ->
                return workerFactory.createWorker(
                    WorkerParams.TrackableRemovalRequested(
                        workResult.trackable, workResult.callbackFunction, workResult.result
                    )
                )
        }
    }
}
