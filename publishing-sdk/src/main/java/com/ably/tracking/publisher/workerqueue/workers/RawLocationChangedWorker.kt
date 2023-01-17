package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class RawLocationChangedWorker(
    private val location: Location,
    private val publisherInteractor: PublisherInteractor,
    private val logHandler: LogHandler?,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {
    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.v("$TAG Raw location changed event received $location")
        properties.lastPublisherLocation = location
        if (properties.areRawLocationsEnabled) {
            val locationUpdate = LocationUpdate(location, emptyList())
            properties.trackables.forEach {
                publisherInteractor.processRawLocationUpdate(locationUpdate, properties, it.id)
            }
        }
        properties.rawLocationChangedCommands.apply {
            if (isNotEmpty()) {
                forEach { command -> command(properties) }
                clear()
            }
        }
        return properties
    }
}
