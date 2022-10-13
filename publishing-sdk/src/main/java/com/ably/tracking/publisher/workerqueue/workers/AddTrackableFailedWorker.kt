package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.AddTrackableFailedWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class AddTrackableFailedWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    val exception: Exception,
    private val ably: Ably,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.isConnectingToAbly) {
            properties.isConnectingToAbly = false
        }

        val failureResult = Result.failure<AddTrackableResult>(exception)
        callbackFunction(failureResult)
        properties.duplicateTrackableGuard.finishAddingTrackable(trackable, failureResult)
        properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))

        if (properties.hasNoTrackablesAddingOrAdded) {
            properties.isStoppingAbly = true
            return SyncAsyncResult(asyncWork = {
                ably.stopConnection()
                AddTrackableFailedWorkResult.StopConnectionCompleted
            })
        }

        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
