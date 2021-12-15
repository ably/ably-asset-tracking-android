package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.AddTrackableCallbackFunction
import com.ably.tracking.publisher.AddTrackableFailedEvent
import com.ably.tracking.publisher.AddTrackableResult
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class AddTrackableFailedWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    val exception: Exception
) : Worker {
    override val event: Request<*>
        get() = AddTrackableFailedEvent(trackable, callbackFunction, exception)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        val failureResult = Result.failure<AddTrackableResult>(exception)
        callbackFunction(failureResult)
        properties.duplicateTrackableGuard.finishAddingTrackable(trackable, failureResult)
        properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))

        return SyncAsyncResult()
    }
}
