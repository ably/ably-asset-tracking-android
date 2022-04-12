package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionReadyResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<ConnectionReadyWorkResult> {
    override fun handle(workResult: ConnectionReadyWorkResult): Worker? {
        when (workResult) {
            is ConnectionReadyWorkResult.RemovalRequested ->
                return workerFactory.createWorker(
                    WorkerParams.TrackableRemovalRequested(
                        workResult.trackable, workResult.callbackFunction, workResult.result
                    )
                )
            is ConnectionReadyWorkResult.NonOptimalConnectionReady ->
                return workerFactory.createWorker(
                    WorkerParams.RetrySubscribeToPresence(
                        workResult.trackable,
                        workResult.presenceUpdateListener
                    )
                )
            is ConnectionReadyWorkResult.OptimalConnectionReady ->
                return null
        }
    }
}
