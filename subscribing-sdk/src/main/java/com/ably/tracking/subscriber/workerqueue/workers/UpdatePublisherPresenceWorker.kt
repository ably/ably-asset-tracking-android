package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdatePublisherPresenceWorker(
    private val presenceMessage: PresenceMessage,
    private val subscriberInteractor: SubscriberInteractor
) : Worker<SubscriberProperties, WorkerSpecification> {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberInteractor.updatePublisherPresence(properties, true)
                    subscriberInteractor.updateTrackableState(properties)
                    subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberInteractor.updatePublisherPresence(properties, false)
                    subscriberInteractor.updateTrackableState(properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
