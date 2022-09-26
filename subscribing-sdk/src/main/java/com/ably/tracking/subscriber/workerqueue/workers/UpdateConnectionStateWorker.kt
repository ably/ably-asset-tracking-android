package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class UpdateConnectionStateWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val subscriberStateManipulator: SubscriberStateManipulator
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        properties.lastConnectionStateChange = connectionStateChange
        subscriberStateManipulator.updateTrackableState(properties)
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
