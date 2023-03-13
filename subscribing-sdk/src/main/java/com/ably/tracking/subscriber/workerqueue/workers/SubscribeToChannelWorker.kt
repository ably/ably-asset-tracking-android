package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeToChannelWorker(
    private val subscriberInteractor: SubscriberInteractor
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        subscriberInteractor.subscribeForChannelState()
        subscriberInteractor.subscribeForEnhancedEvents(properties.presenceData)
        subscriberInteractor.subscribeForRawEvents(properties.presenceData)
        return properties
    }
}
