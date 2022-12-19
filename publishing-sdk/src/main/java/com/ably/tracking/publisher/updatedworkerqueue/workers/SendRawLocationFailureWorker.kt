package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendRawLocationFailureWorker(
    private val locationUpdate: LocationUpdate,
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
        logHandler?.w("$TAG Trackable $trackableId failed to send raw location ${locationUpdate.location}", exception)
        if (properties.rawLocationsPublishingState.shouldRetryPublishing(trackableId)) {
            corePublisher.retrySendingRawLocation(properties, trackableId, locationUpdate)
        } else {
            properties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
            corePublisher.saveRawLocationForFurtherSending(
                properties,
                trackableId,
                locationUpdate.location
            )
            corePublisher.processNextWaitingRawLocationUpdate(properties, trackableId)
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}