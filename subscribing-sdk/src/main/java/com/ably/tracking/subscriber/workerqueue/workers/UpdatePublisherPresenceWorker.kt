package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdatePublisherPresenceWorker(
    private val presenceMessage: PresenceMessage,
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(listOf(presenceMessage))
        return properties
    }
}
