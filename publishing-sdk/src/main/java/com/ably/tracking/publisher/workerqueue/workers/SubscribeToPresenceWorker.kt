package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class SubscribeToPresenceWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val logHandler: LogHandler?,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {
    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        // If the trackable is gone, or marked for removal, stop
        if (
            !properties.trackables.contains(trackable) ||
            properties.trackableRemovalGuard.isMarkedForRemoval(trackable)
        ) {
            return properties
        }

        doAsyncWork {
            val waitForChannelToAttachResult = ably.waitForChannelToAttach(trackable.id)
            if (waitForChannelToAttachResult.isFailure) {
                return@doAsyncWork
            }

            val subscribeToPresenceResult = subscribeToPresenceMessages()
            if (subscribeToPresenceResult.isSuccess) {
                postWork(WorkerSpecification.SubscribeToPresenceSuccess(trackable))
            } else {
                logHandler?.w(
                    "Failed to resubscribe to presence for trackable ${trackable.id}",
                    subscribeToPresenceResult.exceptionOrNull()
                )
                postWork(WorkerSpecification.SubscribeToPresence(trackable, presenceUpdateListener))
            }
        }

        return properties
    }

    private suspend fun subscribeToPresenceMessages(): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener) { result ->
                continuation.resume(result)
            }
        }
    }
}
