package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class StoppingConnectionFinishedWorker() : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.isStoppingAbly = false
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
