package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class DestinationSetWorker(
    private val routeDurationInMilliseconds: Long,
    private val timeProvider: TimeProvider,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.estimatedArrivalTimeInMilliseconds =
            timeProvider.getCurrentTimeInMilliseconds() + routeDurationInMilliseconds
        return properties
    }
}
