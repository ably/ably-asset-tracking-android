package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult

/**
 * A [Worker] interface represents workers which executes synchronous work with an optional synchronous work result
 * and another optional asynchronous work.
 * */
internal interface Worker {
    /**
     * This function is provided in order for implementors  to implement synchronous work. They may  optionally
     * provide result for the executed synchronous work and an optional asynchronous work the form of a suspending
     * function.
     *
     * @param properties : Current state of publisher to be used by this worker. This state is intended to be used
     * inside sync work block. Please use this directly only at synchronus part of code. If you need to pass this
     * into async work lambda, you should copy neccessary properties.
     *
     * @return SyncAsyncResult which represents an optional synchronous work result and an optional asynchronous work
     * **/
    fun doWork(properties: DefaultCorePublisher.Properties): SyncAsyncResult
}
