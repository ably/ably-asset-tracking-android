package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class StoppingConnectionFinishedWorker :
    Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.state = PublisherState.IDLE
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
