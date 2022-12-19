package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class DestinationSetWorker(
    private val routeDurationInMilliseconds: Long,
    private val timeProvider: TimeProvider,
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.estimatedArrivalTimeInMilliseconds =
            timeProvider.getCurrentTimeInMilliseconds() + routeDurationInMilliseconds
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}