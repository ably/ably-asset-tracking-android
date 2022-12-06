package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification

internal class ChannelConnectionStateChangeWorker(
    private val trackableId: String,
    private val connectionStateChange: ConnectionStateChange,
    private val publisher: CorePublisher,
    private val logHandler: LogHandler?
) : Worker<PublisherProperties, WorkerSpecification> {


    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.v("$TAG Trackable $trackableId connection state changed ${connectionStateChange.state}")
        properties.lastChannelConnectionStateChanges[trackableId] = connectionStateChange
        publisher.updateTrackableState(properties, trackableId)
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
