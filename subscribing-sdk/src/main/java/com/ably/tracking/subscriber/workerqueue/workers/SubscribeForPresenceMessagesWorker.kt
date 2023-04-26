package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeForPresenceMessagesWorker(
    private val ably: Ably,
    private val trackableId: String
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        doAsyncWork {
            val currentPresenceMessagesResult = ably.getCurrentPresence(trackableId)
            val initialPresenceMessages: List<PresenceMessage> =
                try {
                    currentPresenceMessagesResult.getOrThrow()
                } catch (_: Throwable) {
                    postDisconnectWork(postWork)
                    return@doAsyncWork
                }

            val subscribeForPresenceMessagesResult = ably.subscribeForPresenceMessages(
                trackableId = trackableId,
                emitCurrentMessages = false,
                listener = { postWork(WorkerSpecification.UpdatePublisherPresence(it)) }
            )
            if (subscribeForPresenceMessagesResult.isFailure) {
                postDisconnectWork(postWork)
                return@doAsyncWork
            }

            postWork(WorkerSpecification.ProcessInitialPresenceMessages(initialPresenceMessages))
        }
        return properties
    }

    private fun postDisconnectWork(postWork: (WorkerSpecification) -> Unit) {
        postWork(
            WorkerSpecification.Disconnect(trackableId) {
                // no-op
            }
        )
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        // All error paths for the async work end in posting the disconnect worker so it feels right to do the same on
        // unexpected errors
        postDisconnectWork(postWork)
    }
}
