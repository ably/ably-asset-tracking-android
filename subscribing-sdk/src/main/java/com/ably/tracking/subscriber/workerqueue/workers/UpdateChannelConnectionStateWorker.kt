package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdateChannelConnectionStateWorker(
    private val channelConnectionStateChange: ConnectionStateChange,
    private val subscriberStateManipulator: SubscriberStateManipulator
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        properties.lastChannelConnectionStateChange = channelConnectionStateChange
        subscriberStateManipulator.updateTrackableState(properties)
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
