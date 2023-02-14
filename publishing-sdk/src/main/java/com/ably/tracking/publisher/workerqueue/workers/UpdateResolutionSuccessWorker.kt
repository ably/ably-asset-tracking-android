package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Resolution
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class UpdateResolutionSuccessWorker(
    private val trackableId: String,
    private val resolution: Resolution
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        properties.removeUpdatingResolution(trackableId, resolution)
        return properties
    }
}
