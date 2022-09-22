package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.Core
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class UpdatePublisherPresenceWorker(
    private val presenceMessage: PresenceMessage,
    private val core: Core
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        when (presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    core.updatePublisherPresence(properties, true)
                    core.updateTrackableState(properties)
                    core.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    core.updatePublisherPresence(properties, false)
                    core.updateTrackableState(properties)
                }
            }
            PresenceAction.UPDATE -> {
                if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                    core.updatePublisherResolutionInformation(presenceMessage.data)
                }
            }
        }
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
