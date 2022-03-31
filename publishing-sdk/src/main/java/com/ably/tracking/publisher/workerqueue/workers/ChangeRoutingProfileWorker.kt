package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class ChangeRoutingProfileWorker(
    private val routingProfile: RoutingProfile,
    private val corePublisher: CorePublisher,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.routingProfile = routingProfile
        properties.currentDestination?.let { corePublisher.setDestination(it, properties) }
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
