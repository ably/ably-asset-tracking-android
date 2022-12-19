package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification

internal class RetrySubscribeToPresenceSuccessWorker(
    private val trackable: Trackable,
    private val publisher: CorePublisher,
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.trackables.contains(trackable)) {
            return properties
        }
        properties.trackableSubscribedToPresenceFlags[trackable.id] = true
        publisher.updateTrackableState(properties, trackable.id)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
