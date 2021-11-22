package com.ably.tracking.publisher.eventqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.AddTrackableHandler
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.eventqueue.AddTrackableResult
import com.ably.tracking.publisher.eventqueue.WorkResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AddTrackableWorker(
    private val publisherState: DefaultCorePublisher.State,
    private val trackable: Trackable,
    private val handler: AddTrackableHandler,
    private val ably: Ably
) : Worker {
    override fun doWork(): (suspend () -> WorkResult)? {
        when {
            publisherState.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                publisherState.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, handler)
            }
            publisherState.trackables.contains(trackable) -> {
                handler(Result.success(publisherState.trackableStateFlows[trackable.id]!!))
            }
            else -> {
                publisherState.duplicateTrackableGuard.startAddingTrackable(trackable)
            }
        }
        return {
            val connectResult = suspendingConnect()
            if (connectResult.isSuccess) {
                AddTrackableResult.Success(trackable, handler)
            } else {
                AddTrackableResult.Fail(trackable, handler, connectResult.exceptionOrNull())
            }
        }
    }

    private suspend fun suspendingConnect(): Result<Boolean> {
        return suspendCoroutine { continuation ->
            ably.connect(trackable.id, publisherState.presenceData, willPublish = true) { result ->
                try {
                    result.getOrThrow()
                    continuation.resume(Result.success(true))
                } catch (exception: ConnectionException) {
                    continuation.resume(Result.failure(exception))
                }
            }
        }
    }
}
