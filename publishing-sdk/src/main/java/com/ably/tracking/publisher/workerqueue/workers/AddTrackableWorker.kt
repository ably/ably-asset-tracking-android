package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.ResultHandler
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val handler: ResultHandler<StateFlow<TrackableState>>,
    private val ably: Ably
) : Worker {
    override fun doWork(properties: DefaultCorePublisher.Properties): SyncAsyncResult {
        return when {
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, handler)
                SyncAsyncResult()
            }
            properties.trackables.contains(trackable) -> {
                return SyncAsyncResult(
                    syncWorkResult = AddTrackableWorkResult.AlreadyIn(
                        properties.trackableStateFlows[trackable.id]!!,
                        handler
                    )
                )
            }
            else -> {
                properties.duplicateTrackableGuard.startAddingTrackable(trackable)
                val presenceData = properties.presenceData.copy()
                SyncAsyncResult(
                    asyncWork = {
                        val connectResult = suspendingConnect(presenceData)
                        if (connectResult.isSuccess) {
                            AddTrackableWorkResult.Success(trackable, handler)
                        } else {
                            AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull(), handler)
                        }
                    }
                )
            }
        }
    }

    private suspend fun suspendingConnect(presenceData: PresenceData): Result<Boolean> {
        return suspendCoroutine { continuation ->
            ably.connect(trackable.id, presenceData, willPublish = true) { result ->
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
