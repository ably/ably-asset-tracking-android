package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendEnhancedLocationFailureWorker(
    private val locationUpdate: EnhancedLocationUpdate,
    private val trackableId: String,
    private val exception: Throwable?,
    private val corePublisher: CorePublisher,
    private val logHandler: LogHandler?,
) : Worker<PublisherProperties, WorkerSpecification> {
    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.w(
            "$TAG Trackable $trackableId failed to send enhanced location ${locationUpdate.location}",
            exception
        )
        val shouldRetryPublishing = properties.enhancedLocationsPublishingState.shouldRetryPublishing(trackableId)
        if (shouldRetryPublishing) {
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
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
