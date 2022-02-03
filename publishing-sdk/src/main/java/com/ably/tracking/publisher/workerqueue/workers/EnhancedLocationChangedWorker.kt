package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.EnhancedLocationChangedEvent
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class EnhancedLocationChangedWorker(
    private val location: Location,
    private val intermediateLocations: List<Location>,
    private val type: LocationUpdateType,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = EnhancedLocationChangedEvent(location, intermediateLocations, type)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.trackables.forEach {
            corePublisher.processEnhancedLocationUpdate(event as EnhancedLocationChangedEvent, properties, it.id)
        }
        corePublisher.updateLocations(EnhancedLocationUpdate(location, emptyList(), intermediateLocations, type))
        corePublisher.checkThreshold(location, properties.active, properties.estimatedArrivalTimeInMilliseconds)
        return SyncAsyncResult()
    }
}
