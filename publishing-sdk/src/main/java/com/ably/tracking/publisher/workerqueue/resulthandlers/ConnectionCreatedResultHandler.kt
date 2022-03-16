package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.TrackableRemovalRequestedEvent
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionCreatedResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<ConnectionCreatedWorkResult> {
    override fun handle(
        workResult: ConnectionCreatedWorkResult,
        corePublisher: CorePublisher,
    ): Worker? {
        when (workResult) {
            is ConnectionCreatedWorkResult.RemovalRequested ->
                corePublisher.request(
                    TrackableRemovalRequestedEvent(
                        trackable = workResult.trackable,
                        callbackFunction = workResult.callbackFunction,
                        result = workResult.result
                    )
                )

            is ConnectionCreatedWorkResult.PresenceSuccess -> {
                return workerFactory.createWorker(
                    WorkerParams.ConnectionReady(
                        workResult.trackable, workResult.callbackFunction, workResult.channelStateChangeListener
                    )
                )
            }
            is ConnectionCreatedWorkResult.PresenceFail ->
                return workerFactory.createWorker(
                    WorkerParams.AddTrackableFailed(
                        workResult.trackable, workResult.callbackFunction, workResult.exception
                    )
                )
        }
        return null
    }
}
