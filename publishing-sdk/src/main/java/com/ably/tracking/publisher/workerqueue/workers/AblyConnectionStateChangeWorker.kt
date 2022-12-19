package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class AblyConnectionStateChangeWorker(
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
        logHandler?.v("$TAG Ably connection state changed ${connectionStateChange.state}")
        properties.lastConnectionStateChange = connectionStateChange
        properties.trackables.forEach {
            publisher.updateTrackableState(properties, it.id)
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
