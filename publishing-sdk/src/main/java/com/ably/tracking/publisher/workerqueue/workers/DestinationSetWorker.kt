package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.SetDestinationSuccessEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class DestinationSetWorker(
    private val routeDurationInMilliseconds: Long,
    private val timeProvider: TimeProvider,
) : Worker {
    override val event: Event
        get() = SetDestinationSuccessEvent(routeDurationInMilliseconds)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.estimatedArrivalTimeInMilliseconds =
            timeProvider.getCurrentTimeInMilliseconds() + routeDurationInMilliseconds
        return SyncAsyncResult()
    }
}
