package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.ConnectionException
import com.ably.tracking.publisher.ConnectionForTrackableReadyEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.TrackableRemovalRequestedEvent
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.WorkResult
import com.ably.tracking.publisher.workerqueue.WorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class ConnectionCreatedResultHandler : WorkResultHandler {
    override fun handle(
        workResult: WorkResult,
        corePublisher: CorePublisher
    ): Worker? {
        when (workResult) {
            is ConnectionCreatedWorkResult.RemovalRequested ->
                corePublisher.request(
                    TrackableRemovalRequestedEvent(
                        trackable = workResult.trackable,
                        callbackFunction = workResult.callbackFunction,
                        result = if (workResult.successfulDisconnect) Result.success(Unit) else Result.failure(
                            workResult.exception
                                as ConnectionException
                        )
                    )
                )

            is ConnectionCreatedWorkResult.PresenceSuccess -> {
                corePublisher.request(
                    ConnectionForTrackableReadyEvent(
                        workResult.trackable,
                        workResult.callbackFunction
                    )
                )
            }
            is ConnectionCreatedWorkResult.PresenceFail ->
                return AddTrackableFailedWorker(workResult.trackable, workResult.callbackFunction, workResult.exception)
        }
        return null
    }
}
