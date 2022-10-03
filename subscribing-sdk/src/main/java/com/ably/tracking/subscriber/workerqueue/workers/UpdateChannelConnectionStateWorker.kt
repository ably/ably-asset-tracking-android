package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdateChannelConnectionStateWorker(
    private val channelConnectionStateChange: ConnectionStateChange,
    private val subscriberInteractor: SubscriberInteractor
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): Properties {
        properties.lastChannelConnectionStateChange = channelConnectionStateChange
        subscriberInteractor.updateTrackableState(properties)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
