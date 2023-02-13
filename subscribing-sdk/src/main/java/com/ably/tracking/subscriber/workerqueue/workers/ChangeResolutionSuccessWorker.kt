package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Resolution
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class ChangeResolutionSuccessWorker(
    private val trackableId: String,
    private val resolution: Resolution?
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.removeUpdatingResolution(trackableId, resolution)
        return properties
    }
}
