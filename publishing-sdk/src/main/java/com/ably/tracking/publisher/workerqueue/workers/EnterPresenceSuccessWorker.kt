package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

/**
 * A worker that is called when entering presence results
 * in success.
 */
internal class EnterPresenceSuccessWorker(
    private val trackable: Trackable,
    private val publisherInteractor: PublisherInteractor,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.trackables.contains(trackable)) {
            return properties
        }

        properties.trackableEnteredPresenceFlags[trackable.id] = true
        publisherInteractor.resolveResolution(trackable, properties)
        publisherInteractor.updateTrackableState(properties, trackable.id)
        return properties
    }
}
