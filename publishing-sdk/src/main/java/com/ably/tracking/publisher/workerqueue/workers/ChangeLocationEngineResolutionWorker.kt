package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class ChangeLocationEngineResolutionWorker(
    private val policy: ResolutionPolicy,
    private val mapbox: Mapbox,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (!properties.isLocationEngineResolutionConstant) {
            val newResolution = policy.resolve(properties.resolutions.values.toSet())
            properties.locationEngineResolution = newResolution
            mapbox.changeResolution(newResolution)
        }
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
