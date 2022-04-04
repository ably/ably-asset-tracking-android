package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.RetrySubscribeToPresenceWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class RetrySubscribeToPresenceWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (!properties.trackables.contains(trackable)) {
            return SyncAsyncResult(RetrySubscribeToPresenceWorkResult.TrackableRemoved)
        }
        return SyncAsyncResult(
            asyncWork = {
                val waitForChannelToBeConnectedResult = waitForChannelToBeConnected(trackable)
                if (waitForChannelToBeConnectedResult.isFailure) {
                    return@SyncAsyncResult RetrySubscribeToPresenceWorkResult.ChannelFailed
                }

                val subscribeToPresenceResult = subscribeToPresenceMessages()
                return@SyncAsyncResult if (subscribeToPresenceResult.isSuccess) {
                    RetrySubscribeToPresenceWorkResult.Success(trackable)
                } else {
                    RetrySubscribeToPresenceWorkResult.Failure(trackable, presenceUpdateListener)
                }
            }
        )
    }

    private suspend fun subscribeToPresenceMessages(): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener) { result ->
                continuation.resume(result)
            }
        }
    }

    private suspend fun waitForChannelToBeConnected(trackable: Trackable): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.subscribeForChannelStateChange(trackable.id) {
                if (it.state == ConnectionState.ONLINE) {
                    continuation.resume(Result.success(Unit))
                }
                if (it.state == ConnectionState.FAILED) {
                    continuation.resume(Result.failure(Exception("Channel failed")))
                }
            }
        }
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
