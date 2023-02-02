package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

/**
 * A worker called to either confirm that presence has been entered,
 * or retry entering presence if not.
 */
internal class EnterPresenceWorker(
    private val trackable: Trackable,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (
            properties.trackables.contains(trackable) &&
            !properties.trackableRemovalGuard.isMarkedForRemoval(trackable)
        ) {
            postWork(WorkerSpecification.RetryEnterPresence(trackable))
        }

        return properties
    }
}
