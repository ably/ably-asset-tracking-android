package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.ChannelConnectionStateChangeEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class ChannelConnectionStateChangeWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val trackableId: String,
    private val corePublisher: CorePublisher,
    private val logHandler: LogHandler?,
) : Worker {
    private val TAG = createLoggingTag(this)
    override val event: Event
        get() = ChannelConnectionStateChangeEvent(connectionStateChange, trackableId)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        logHandler?.v("$TAG Trackable $trackableId connection state changed ${connectionStateChange.state}")
        properties.lastChannelConnectionStateChanges[trackableId] = connectionStateChange
        corePublisher.updateTrackableState(properties, trackableId)
        return SyncAsyncResult()
    }
}
