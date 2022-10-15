package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.results.TrackableRemovalRequestedWorkResult
import kotlinx.coroutines.flow.StateFlow

internal class TrackableRemovalRequestedWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val ably: Ably,
    private val result: Result<Unit>
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (result.isSuccess) {
            properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))
        } else {
            properties.trackableRemovalGuard.removeMarked(trackable, Result.failure(result.exceptionOrNull()!!))
        }
        callbackFunction(Result.failure(RemoveTrackableRequestedException()))
        properties.duplicateTrackableGuard.finishAddingTrackable(
            trackable,
            Result.failure(RemoveTrackableRequestedException())
        )
        val removedTheLastTrackable = properties.hasNoTrackablesAddingOrAdded
        if (removedTheLastTrackable) {
            properties.state = PublisherState.DISCONNECTING
            return SyncAsyncResult(asyncWork = {
                ably.stopConnection()
                TrackableRemovalRequestedWorkResult.StopConnectionCompleted
            })
        }
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
