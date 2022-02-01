package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.SendEnhancedLocationSuccessEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendEnhancedLocationSuccessWorker(
    private val location: Location,
    private val trackableId: String,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = SendEnhancedLocationSuccessEvent(location, trackableId)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.enhancedLocationsPublishingState.unmarkMessageAsPending(trackableId)
        properties.lastSentEnhancedLocations[trackableId] = location
        properties.skippedEnhancedLocations.clear(trackableId)
        corePublisher.updateTrackableState(properties, trackableId)
        corePublisher.processNextWaitingEnhancedLocationUpdate(properties, trackableId)
        return SyncAsyncResult()
    }
}
