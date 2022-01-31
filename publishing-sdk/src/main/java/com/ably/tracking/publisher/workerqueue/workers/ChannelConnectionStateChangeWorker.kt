package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.ChannelConnectionStateChangeEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class ChannelConnectionStateChangeWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val trackableId: String,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = ChannelConnectionStateChangeEvent(connectionStateChange, trackableId)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.lastChannelConnectionStateChanges[trackableId] = connectionStateChange
        corePublisher.updateTrackableState(properties, trackableId)
        return SyncAsyncResult()
    }
}
