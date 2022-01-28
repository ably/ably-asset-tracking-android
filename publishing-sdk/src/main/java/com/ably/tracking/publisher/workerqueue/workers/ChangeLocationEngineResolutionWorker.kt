package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.ChangeLocationEngineResolutionEvent
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class ChangeLocationEngineResolutionWorker(
    private val policy: ResolutionPolicy,
    private val mapbox: Mapbox,
) : Worker {
    override val event: Event
        get() = ChangeLocationEngineResolutionEvent

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        val newResolution = policy.resolve(properties.resolutions.values.toSet())
        properties.locationEngineResolution = newResolution
        mapbox.changeResolution(newResolution)
        return SyncAsyncResult()
    }
}
