package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RawLocationChangedEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RawLocationChangedWorker(
    private val location: Location,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = RawLocationChangedEvent(location)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.lastPublisherLocation = location
        if (properties.areRawLocationsEnabled) {
            properties.trackables.forEach {
                corePublisher.processRawLocationUpdate(event as RawLocationChangedEvent, properties, it.id)
            }
        }
        properties.rawLocationChangedCommands.apply {
            if (isNotEmpty()) {
                forEach { command -> command(properties) }
                clear()
            }
        }
        return SyncAsyncResult()
    }
}
