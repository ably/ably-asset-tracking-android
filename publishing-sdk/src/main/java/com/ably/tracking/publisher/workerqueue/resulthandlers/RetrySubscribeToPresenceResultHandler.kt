package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.RetrySubscribeToPresenceWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class RetrySubscribeToPresenceResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<RetrySubscribeToPresenceWorkResult> {
    override fun handle(workResult: RetrySubscribeToPresenceWorkResult): Worker? {
        return when (workResult) {
            is RetrySubscribeToPresenceWorkResult.Failure ->
                workerFactory.createWorker(
                    WorkerParams.RetrySubscribeToPresence(
                        workResult.trackable,
                        workResult.presenceUpdateListener,
                    )
                )
            is RetrySubscribeToPresenceWorkResult.Success ->
                workerFactory.createWorker(
                    WorkerParams.RetrySubscribeToPresenceSuccess(
                        workResult.trackable,
                    )
                )
            RetrySubscribeToPresenceWorkResult.ChannelFailed -> null
            RetrySubscribeToPresenceWorkResult.TrackableRemoved -> null
        }
    }
}
