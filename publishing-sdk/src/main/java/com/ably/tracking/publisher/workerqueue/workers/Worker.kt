package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.WorkResult

/**
 * This interface is for workers whose sole purpose is to process works that were enqueued in [WorkerQueue]
 * */
internal interface Worker {
    /**
     * This function is provided in order for implementors  to implement synchronous work (such as state read and
     * manipulation)
     * @return an optional suspending function that returns a [WorkResult]. This suspending function is intended for
     * [WorkerQueue] to process asynchornous operations.
     * **/
    fun doWork(publisherState: DefaultCorePublisher.State): SyncAsyncResult
}



