package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.SendEnhancedLocationFailureEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendEnhancedLocationFailureWorker(
    private val locationUpdate: EnhancedLocationUpdate,
    private val trackableId: String,
    private val exception: Throwable?,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = SendEnhancedLocationFailureEvent(locationUpdate, trackableId, exception)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.enhancedLocationsPublishingState.shouldRetryPublishing(trackableId)) {
            corePublisher.retrySendingEnhancedLocation(properties, trackableId, locationUpdate)
        } else {
            properties.enhancedLocationsPublishingState.unmarkMessageAsPending(trackableId)
            corePublisher.saveEnhancedLocationForFurtherSending(
                properties,
                trackableId,
                locationUpdate.location
            )
            corePublisher.processNextWaitingEnhancedLocationUpdate(properties, trackableId)
        }
        return SyncAsyncResult()
    }
}