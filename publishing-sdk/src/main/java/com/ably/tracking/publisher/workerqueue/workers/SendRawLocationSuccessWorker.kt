package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class SendRawLocationSuccessWorker(
    private val location: Location,
    private val trackableId: String,
    private val publisherInteractor: PublisherInteractor,
    private val logHandler: LogHandler?,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {
    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.v("$TAG Trackable $trackableId successfully sent raw location $location")
        properties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        properties.lastSentRawLocations[trackableId] = location
        properties.skippedRawLocations.clear(trackableId)
        publisherInteractor.processNextWaitingRawLocationUpdate(properties, trackableId)
        return properties
    }
}
