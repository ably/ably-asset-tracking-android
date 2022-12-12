package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RefreshResolutionPolicyWorker(
    private val corePublisher: CorePublisher,
) : Worker<PublisherProperties, WorkerSpecification> {
    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.trackables.forEach { corePublisher.resolveResolution(it, properties) }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
