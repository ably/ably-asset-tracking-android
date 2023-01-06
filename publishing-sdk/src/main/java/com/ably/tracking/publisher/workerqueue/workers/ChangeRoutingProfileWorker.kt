package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ChangeRoutingProfileWorker(
    private val routingProfile: RoutingProfile,
    private val publisherInteractor: PublisherInteractor,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.routingProfile = routingProfile
        properties.currentDestination?.let { publisherInteractor.setDestination(it, properties) }
        return properties
    }
}
