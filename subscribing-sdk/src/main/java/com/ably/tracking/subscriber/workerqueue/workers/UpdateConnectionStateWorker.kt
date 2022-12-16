package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdateConnectionStateWorker(
    private val connectionStateChange: ConnectionStateChange,
) : Worker<SubscriberProperties, WorkerSpecification> {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.updateForConnectionStateChangeAndThenEmitEventsIfRequired(connectionStateChange)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
