package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification

internal class EnhancedLocationChangedWorker(
    private val location: Location,
    private val intermediateLocations: List<Location>,
    private val type: LocationUpdateType,
    private val corePublisher: CorePublisher,
    private val logHandler: LogHandler?,
) : Worker<PublisherProperties, WorkerSpecification> {
    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.v("$TAG Enhanced location changed event received $location")
        val enhancedLocationUpdate = EnhancedLocationUpdate(location, emptyList(), intermediateLocations, type)
        properties.trackables.forEach {
            corePublisher.processEnhancedLocationUpdate(enhancedLocationUpdate, properties, it.id)
        }
        corePublisher.updateLocations(EnhancedLocationUpdate(location, emptyList(), intermediateLocations, type))
        corePublisher.checkThreshold(location, properties.active, properties.estimatedArrivalTimeInMilliseconds)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
