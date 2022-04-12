package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class RetrySubscribeToPresenceSuccessWorker(
    private val trackable: Trackable,
    private val publisher: CorePublisher,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (!properties.trackables.contains(trackable)) {
            return SyncAsyncResult()
        }
        properties.trackableSubscribedToPresenceFlags[trackable.id] = true
        publisher.updateTrackableState(properties, trackable.id)
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
