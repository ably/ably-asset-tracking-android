package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class SendEnhancedLocationFailureWorker(
    private val locationUpdate: EnhancedLocationUpdate,
    private val trackableId: String,
    private val exception: Throwable?,
    private val publisherInteractor: PublisherInteractor,
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
            publisherInteractor.retrySendingEnhancedLocation(properties, trackableId, locationUpdate)
        } else {
            properties.enhancedLocationsPublishingState.unmarkMessageAsPending(trackableId)
            publisherInteractor.saveEnhancedLocationForFurtherSending(
                properties,
                trackableId,
                locationUpdate.location
            )
            publisherInteractor.processNextWaitingEnhancedLocationUpdate(properties, trackableId)
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
