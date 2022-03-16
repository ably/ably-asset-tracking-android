package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.ChangeLocationEngineResolutionEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.RemoveTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class RemoveTrackableResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<RemoveTrackableWorkResult> {
    override fun handle(
        workResult: RemoveTrackableWorkResult,
        corePublisher: CorePublisher,
    ): Worker? {
        when (workResult) {
            is RemoveTrackableWorkResult.Success ->
                workerFactory.createWorker(
                    WorkerParams.DisconnectSuccess(
                        trackable = workResult.trackable,
                        callbackFunction = {
                            if (it.isSuccess) {
                                workResult.callbackFunction(Result.success(true))
                            } else {
                                workResult.callbackFunction(Result.failure(it.exceptionOrNull()!!))
                            }
                        },
                        shouldRecalculateResolutionCallback = {
                            corePublisher.enqueue(ChangeLocationEngineResolutionEvent)
                        }
                    )
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
