package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

/**
 * Removes a trackable from the removal guard to signal success.
 */
internal class TrackableRemovalSuccessWorker(
    private val trackable: Trackable,
    private val result: Result<Boolean>
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {
    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.trackableRemovalGuard.removeMarked(trackable, result)

        return properties
    }
}
