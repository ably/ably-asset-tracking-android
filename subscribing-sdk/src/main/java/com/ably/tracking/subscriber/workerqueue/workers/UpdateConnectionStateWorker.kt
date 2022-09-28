package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdateConnectionStateWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val subscriberInteractor: SubscriberInteractor
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        properties.lastConnectionStateChange = connectionStateChange
        subscriberInteractor.updateTrackableState(properties)
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
