package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.SendRawLocationSuccessEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendRawLocationSuccessWorker(
    private val location: Location,
    private val trackableId: String,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = SendRawLocationSuccessEvent(location, trackableId)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        properties.lastSentRawLocations[trackableId] = location
        properties.skippedRawLocations.clear(trackableId)
        corePublisher.processNextWaitingRawLocationUpdate(properties, trackableId)
        return SyncAsyncResult()
    }
}
