package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdatePublisherPresenceWorker(
    private val presenceMessage: PresenceMessage,
    private val subscriberStateManipulator: SubscriberStateManipulator
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberStateManipulator.updatePublisherPresence(properties, true)
                    subscriberStateManipulator.updateTrackableState(properties)
                    subscriberStateManipulator.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberStateManipulator.updatePublisherPresence(properties, false)
                    subscriberStateManipulator.updateTrackableState(properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    subscriberStateManipulator.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
        }
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
