package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ChangeRoutingProfileWorker(
    private val routingProfile: RoutingProfile,
    private val corePublisher: CorePublisher,
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.routingProfile = routingProfile
        properties.currentDestination?.let { corePublisher.setDestination(it, properties) }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
