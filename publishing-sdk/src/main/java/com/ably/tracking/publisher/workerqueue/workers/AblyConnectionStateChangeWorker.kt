package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class AblyConnectionStateChangeWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val publisherInteractor: PublisherInteractor,
    private val logHandler: LogHandler?
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    private val TAG = createLoggingTag(this)

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        logHandler?.v("$TAG Ably connection state changed ${connectionStateChange.state}")
        properties.lastConnectionStateChange = connectionStateChange
        properties.trackables.forEach {
            publisherInteractor.updateTrackableState(properties, it.id)
        }
        return properties
    }
}
