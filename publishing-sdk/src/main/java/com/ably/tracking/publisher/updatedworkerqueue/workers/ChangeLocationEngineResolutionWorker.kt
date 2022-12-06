package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification

internal class ChangeLocationEngineResolutionWorker(
    private val policy: ResolutionPolicy,
    private val mapbox: Mapbox,
) : Worker<PublisherProperties, WorkerSpecification> {

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

    override fun doWhenStopped(exception: Exception) = Unit
}
