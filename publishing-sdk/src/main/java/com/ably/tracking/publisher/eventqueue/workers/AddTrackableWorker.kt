package com.ably.tracking.publisher.eventqueue.workers

import android.util.Log
import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.eventqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.eventqueue.SyncAsyncResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "AddTrackableWorker"

internal class AddTrackableWorker(
    private val publisherState: DefaultCorePublisher.State,
    private val trackable: Trackable,
    private val ably: Ably
) : Worker {
    override fun doWork(): SyncAsyncResult {
        Log.d(TAG, "doWork: ")
        if (publisherState.trackables.contains(trackable)) {
            return SyncAsyncResult(
                AddTrackableWorkResult.AlreadyIn(
                    publisherState.trackableStateFlows[trackable
                        .id]!!
                ), null
            )
        }
        return SyncAsyncResult(null, asyncWork = {
            val connectResult = suspendingConnect()
            Log.d(TAG, "doWork: connect result received $connectResult")
            if (connectResult.isSuccess) {
                AddTrackableWorkResult.Success(trackable)
            } else {
                AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull())
            }
        })
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

    override fun hashCode(): Int {
        return trackable.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddTrackableWorker

        if (trackable != other.trackable) return false

        return true
    }
}
