package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.processPresenceMessage
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
        processPresenceMessage(presenceMessage, properties, subscriberInteractor)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
