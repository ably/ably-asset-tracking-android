package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.RemoveTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.workers.DisconnectSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class RemoveTrackableResultHandler : WorkResultHandler<RemoveTrackableWorkResult> {
    override fun handle(
        workResult: RemoveTrackableWorkResult,
        corePublisher: CorePublisher
    ): Worker? {
        when (workResult) {
            is RemoveTrackableWorkResult.Success ->
                DisconnectSuccessWorker(
                    trackable = workResult.trackable,
                    callbackFunction = {
                        if (it.isSuccess) {
                            workResult.callbackFunction(Result.success(true))
                        } else {
                            workResult.callbackFunction(Result.failure(it.exceptionOrNull()!!))
                        }
                    },
                    corePublisher = corePublisher,
                )
            is RemoveTrackableWorkResult.Fail -> workResult.callbackFunction(
                Result.failure(workResult.exception)
            )
            is RemoveTrackableWorkResult.NotPresent -> workResult.callbackFunction(
                // notify with false to indicate that it was not removed
                Result.success(false)
            )
        }
        return null
    }
}
