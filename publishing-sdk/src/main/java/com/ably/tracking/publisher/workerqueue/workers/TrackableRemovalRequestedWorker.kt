package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.TrackableRemovalRequestedEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.results.TrackableRemovalRequestedResult
import kotlinx.coroutines.flow.StateFlow

internal class TrackableRemovalRequestedWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val result: Result<Unit>
) : Worker {
    override val event: Event
        get() = TrackableRemovalRequestedEvent(trackable, callbackFunction, result)

    override suspend fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (result.isSuccess) {
            properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))
            return SyncAsyncResult(TrackableRemovalRequestedResult.Success(trackable))
        } else {
            properties.trackableRemovalGuard.removeMarked(
                trackable,
                Result.failure(result.exceptionOrNull()!!)
            )
            // in the original code this was out of else block but it didn't make sense to run this on success case as
            // there will not be an exception. When reviewing please consider any side effect.
            properties.duplicateTrackableGuard.finishAddingTrackable(
                trackable,
                Result.failure(RemoveTrackableRequestedException())
            )
            return SyncAsyncResult(TrackableRemovalRequestedResult.Fail(callbackFunction))
        }
    }
}
