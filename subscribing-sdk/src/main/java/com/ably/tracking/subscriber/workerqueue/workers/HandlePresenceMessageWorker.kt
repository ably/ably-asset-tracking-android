package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class HandlePresenceMessageWorker(
    private val presenceMessage: PresenceMessage,
    private val coreSubscriber: CoreSubscriber
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    coreSubscriber.updatePublisherPresence(properties, true)
                    coreSubscriber.updateTrackableState(properties)
                    coreSubscriber.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    coreSubscriber.updatePublisherPresence(properties, false)
                    coreSubscriber.updateTrackableState(properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    coreSubscriber.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
        }
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
