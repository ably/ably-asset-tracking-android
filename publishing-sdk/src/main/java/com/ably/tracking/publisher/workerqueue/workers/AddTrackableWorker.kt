package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val ably: Ably
) : Worker {
    override fun doWork(properties: DefaultCorePublisher.Properties): SyncAsyncResult {
        return when {
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, callbackFunction)
                SyncAsyncResult()
            }
            properties.trackables.contains(trackable) -> {
                callbackFunction(Result.success(properties.trackableStateFlows[trackable.id]!!))
                SyncAsyncResult()
            }
            properties.trackables.contains(trackable) -> {
                return SyncAsyncResult(
                    AddTrackableWorkResult.AlreadyIn(properties.trackableStateFlows[trackable.id]!!, callbackFunction),
                    null
                )
            }
            else ->
                return SyncAsyncResult(
                    syncWorkResult = null,
                    asyncWork = {
                        val connectResult = suspendingConnect(properties)
                        if (connectResult.isSuccess) {
                            AddTrackableWorkResult.Success(trackable, callbackFunction)
                        } else {
                            AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull(), callbackFunction)
                        }
                    }
                )
        }
    }

    private suspend fun suspendingConnect(publisherState: DefaultCorePublisher.Properties): Result<Boolean> {
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
