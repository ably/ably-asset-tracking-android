package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionCreatedResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<ConnectionCreatedWorkResult> {
    override fun handle(workResult: ConnectionCreatedWorkResult): Worker? {
        when (workResult) {
            is ConnectionCreatedWorkResult.RemovalRequested ->
                return workerFactory.createWorker(
                    WorkerParams.TrackableRemovalRequested(
                        workResult.trackable, workResult.callbackFunction, workResult.result
                    )
                )

            is ConnectionCreatedWorkResult.PresenceSuccess -> {
                return workerFactory.createWorker(
                    WorkerParams.ConnectionReady(
                        workResult.trackable,
                        workResult.callbackFunction,
                        workResult.channelStateChangeListener,
                        workResult.presenceUpdateListener,
                        isSubscribedToPresence = true,
                    )
                )
            }
            is ConnectionCreatedWorkResult.PresenceFail ->
                return workerFactory.createWorker(
                    WorkerParams.ConnectionReady(
                        workResult.trackable,
                        workResult.callbackFunction,
                        workResult.channelStateChangeListener,
                        workResult.presenceUpdateListener,
                        isSubscribedToPresence = false,
                    )
                )
        }
    }
}
