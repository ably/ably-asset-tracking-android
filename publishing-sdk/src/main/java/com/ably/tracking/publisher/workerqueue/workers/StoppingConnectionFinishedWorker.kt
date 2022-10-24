package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class StoppingConnectionFinishedWorker() : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.state = PublisherState.IDLE
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
