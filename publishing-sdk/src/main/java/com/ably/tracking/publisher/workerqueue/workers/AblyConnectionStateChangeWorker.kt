package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.AblyConnectionStateChangeEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class AblyConnectionStateChangeWorker(
    private val connectionStateChange: ConnectionStateChange,
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = AblyConnectionStateChangeEvent(connectionStateChange)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.lastConnectionStateChange = connectionStateChange
        properties.trackables.forEach {
            corePublisher.updateTrackableState(properties, it.id)
        }
        return SyncAsyncResult()
    }
}
