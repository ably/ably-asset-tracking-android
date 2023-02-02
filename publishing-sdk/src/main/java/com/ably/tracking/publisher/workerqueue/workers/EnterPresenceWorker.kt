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
    private val enteredPresenceOnConnect: Boolean,
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
            /**
             * If ably.connect resulted in success, then presence has already
             * been entered, so we can succeed and stop here.
             */
            if (enteredPresenceOnConnect) {
                postWork(WorkerSpecification.EnterPresenceSuccess(trackable))
                return properties
            }

            // Otherwise, we'll do retries until we have succeeded
            postWork(WorkerSpecification.RetryEnterPresence(trackable))
        }

        return properties
    }
}
