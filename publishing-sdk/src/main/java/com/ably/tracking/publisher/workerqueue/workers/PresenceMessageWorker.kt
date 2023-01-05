package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class PresenceMessageWorker(
    private val trackable: Trackable,
    private val presenceMessage: PresenceMessage,
    private val publisherInteractor: PublisherInteractor
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    publisherInteractor.addSubscriber(
                        presenceMessage.memberKey,
                        trackable,
                        presenceMessage.data,
                        properties
                    )
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    publisherInteractor.removeSubscriber(presenceMessage.memberKey, trackable, properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    publisherInteractor.updateSubscriber(
                        presenceMessage.memberKey,
                        trackable,
                        presenceMessage.data,
                        properties
                    )
                }
            }
        }
        return properties
    }
}
