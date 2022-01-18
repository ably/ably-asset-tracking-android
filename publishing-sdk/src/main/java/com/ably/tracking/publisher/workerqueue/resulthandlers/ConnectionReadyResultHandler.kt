package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.workers.TrackableRemovalRequestedWorker
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionReadyResultHandler : WorkResultHandler<ConnectionReadyWorkResult> {
    override fun handle(workResult: ConnectionReadyWorkResult, corePublisher: CorePublisher): Worker? {
        when (workResult) {
            is ConnectionReadyWorkResult.RemovalRequested ->
                return TrackableRemovalRequestedWorker(
                    trackable = workResult.trackable,
                    callbackFunction = workResult.callbackFunction,
                    result = workResult.result
                )
        }
    }
}
