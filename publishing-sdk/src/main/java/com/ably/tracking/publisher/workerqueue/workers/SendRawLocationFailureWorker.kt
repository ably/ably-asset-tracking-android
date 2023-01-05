package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class SendRawLocationFailureWorker(
    private val locationUpdate: LocationUpdate,
    private val trackableId: String,
    private val exception: Throwable?,
    private val publisherInteractor: PublisherInteractor,
    private val logHandler: LogHandler?,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {
    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.w("$TAG Trackable $trackableId failed to send raw location ${locationUpdate.location}", exception)
        if (properties.rawLocationsPublishingState.shouldRetryPublishing(trackableId)) {
            publisherInteractor.retrySendingRawLocation(properties, trackableId, locationUpdate)
        } else {
            properties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
            publisherInteractor.saveRawLocationForFurtherSending(
                properties,
                trackableId,
                locationUpdate.location
            )
            publisherInteractor.processNextWaitingRawLocationUpdate(properties, trackableId)
        }
        return properties
    }
}
