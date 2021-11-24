package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult

/**
 * A [Worker] interface represents workers which executes synchronous work with an optional synchronous work result
 * and another optional asynchrnous work.
 * */
internal interface Worker {
    /**
     * This function is provided in order for implementors  to implement synchronous work, optionally provide a
     * result for synchronous work and an optional asynchronous work.
     * @return [SyncAsyncResult] which represent an optional synchronous work result and an optional asynchronous work
     * [properties] : Current state of publisher to be used by this worker. Please do not modify and access to
     * this from async work block.
     * **/
    fun doWork(properties: DefaultCorePublisher.Properties): SyncAsyncResult
}
