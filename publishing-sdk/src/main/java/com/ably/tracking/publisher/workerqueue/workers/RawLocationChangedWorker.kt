package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RawLocationChangedEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RawLocationChangedWorker(
    private val location: Location,
    private val corePublisher: CorePublisher,
    private val logHandler: LogHandler?,
) : Worker {
    private val TAG = createLoggingTag(this)
    override val event: Event
        get() = RawLocationChangedEvent(location)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        logHandler?.v("$TAG Raw location changed event received $location")
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
