package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RefreshResolutionPolicyEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RefreshResolutionPolicyWorker(
    private val corePublisher: CorePublisher,
) : Worker {
    override val event: Event
        get() = RefreshResolutionPolicyEvent()

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.trackables.forEach { corePublisher.resolveResolution(it, properties) }
        return SyncAsyncResult()
    }
}
