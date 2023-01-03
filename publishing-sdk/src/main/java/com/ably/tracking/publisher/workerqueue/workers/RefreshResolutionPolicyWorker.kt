package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class RefreshResolutionPolicyWorker(
    private val publisherInteractor: PublisherInteractor,
) : Worker<PublisherProperties, WorkerSpecification> {
    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.trackables.forEach { publisherInteractor.resolveResolution(it, properties) }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
