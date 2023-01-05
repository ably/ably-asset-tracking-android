package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class RetrySubscribeToPresenceWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val logHandler: LogHandler?,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
) : Worker<PublisherProperties, WorkerSpecification> {
    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.trackables.contains(trackable)) {
            return properties
        }

        doAsyncWork {
            val waitForChannelToBeConnectedResult = waitForChannelToBeConnected(trackable)
            if (waitForChannelToBeConnectedResult.isFailure) {
                return@doAsyncWork
            }

            val subscribeToPresenceResult = ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener)
            if (subscribeToPresenceResult.isSuccess) {
                postWork(WorkerSpecification.RetrySubscribeToPresenceSuccess(trackable))
            } else {
                logHandler?.w(
                    "Failed to resubscribe to presence for trackable ${trackable.id}",
                    subscribeToPresenceResult.exceptionOrNull()
                )
                postWork(
                    WorkerSpecification.RetrySubscribeToPresence(trackable, presenceUpdateListener)
                )
            }
        }

        return properties
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
