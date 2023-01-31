package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ErrorInformation
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class FailTrackableWorker(
    private val trackable: Trackable,
    private val errorInformation: ErrorInformation,
    private val ably: Ably,
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

        // set trackable state
        publisherInteractor.setFinalTrackableState(properties, trackable.id, TrackableState.Failed(errorInformation))
        doAsyncWork {
            ably.disconnect(trackable.id, properties.presenceData)
        }
        return properties
    }
}
