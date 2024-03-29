package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ChangeLocationEngineResolutionWorker(
    private val policy: ResolutionPolicy,
    private val mapbox: Mapbox,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.isLocationEngineResolutionConstant) {
            val newResolution = policy.resolve(properties.resolutions.values.toSet())
            properties.locationEngineResolution = newResolution
            mapbox.changeResolution(newResolution)
        }
        return properties
    }
}
