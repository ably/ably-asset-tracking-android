package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class DestinationSetWorker(
    private val routeDurationInMilliseconds: Long,
    private val timeProvider: TimeProvider,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.estimatedArrivalTimeInMilliseconds =
            timeProvider.getCurrentTimeInMilliseconds() + routeDurationInMilliseconds
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
