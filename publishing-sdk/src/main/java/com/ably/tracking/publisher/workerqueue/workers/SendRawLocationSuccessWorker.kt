package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SendRawLocationSuccessWorker(
    private val location: Location,
    private val trackableId: String,
    private val corePublisher: CorePublisher,
    private val logHandler: LogHandler?,
) : Worker {
    private val TAG = createLoggingTag(this)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        logHandler?.v("$TAG Trackable $trackableId successfully sent raw location $location")
        properties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        properties.lastSentRawLocations[trackableId] = location
        properties.skippedRawLocations.clear(trackableId)
        corePublisher.processNextWaitingRawLocationUpdate(properties, trackableId)
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
