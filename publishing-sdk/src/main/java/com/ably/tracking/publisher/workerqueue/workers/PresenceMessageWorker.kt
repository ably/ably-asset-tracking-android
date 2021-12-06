package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PresenceMessageEvent
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult

internal class PresenceMessageWorker(
    private val trackable: Trackable,
    private val presenceMessage: PresenceMessage,
    private val corePublisher: CorePublisher
) :
    Worker {
    override val event: Event
        get() = PresenceMessageEvent(trackable, presenceMessage)

    override suspend fun doWork(properties: PublisherProperties): SyncAsyncResult {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    corePublisher.addSubscriber(
                        presenceMessage.clientId,
                        trackable,
                        presenceMessage.data,
                        properties
                    )
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    corePublisher.removeSubscriber(presenceMessage.clientId, trackable, properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    corePublisher.updateSubscriber(
                        presenceMessage.clientId,
                        trackable,
                        presenceMessage.data,
                        properties
                    )
                }
            }
        }
        return SyncAsyncResult()
    }
}
