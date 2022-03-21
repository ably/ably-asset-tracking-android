package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RemoveTrackableEvent
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.RemoveTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RemoveTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<Boolean>,
    private val ably: Ably
) : Worker {
    override val event: Request<*>
        get() = RemoveTrackableEvent(trackable, callbackFunction)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        return when {
            properties.trackables.contains(trackable) -> {
                val presenceData = properties.presenceData.copy()
                SyncAsyncResult(
                    asyncWork = {
                        // Leave Ably channel.
                        val result = ably.disconnect(trackable.id, presenceData)
                        if (result.isSuccess) {
                            RemoveTrackableWorkResult.Success(callbackFunction, trackable)
                        } else {
                            RemoveTrackableWorkResult.Fail(callbackFunction, result.exceptionOrNull()!!)
                        }
                    }
                )
            }
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.trackableRemovalGuard.markForRemoval(trackable, callbackFunction)
                SyncAsyncResult()
            }
            else -> {
                SyncAsyncResult(RemoveTrackableWorkResult.NotPresent(callbackFunction))
            }
        }
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
