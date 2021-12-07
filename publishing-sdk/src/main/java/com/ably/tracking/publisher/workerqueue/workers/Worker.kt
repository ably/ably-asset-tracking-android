package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

/**
 * A [Worker] interface represents workers which executes synchronous work with an optional synchronous work result
 * and another optional asynchronous work.
 * */
internal interface Worker {
    /**
     * A temporary variable that enables sending requests in form of events to CorePublisher, during refactoring
     *
     * This should be removed after refactoring finishes
     * */
    val event: Event

    /**
     * This function is provided in order for implementors  to implement synchronous work. They may  optionally
     * provide result for the executed synchronous work and an optional asynchronous work the form of a suspending
     * function.
     *
     * @param properties : Current state of publisher to be used by this worker.
     *
     * Please use [properties] directly only at synchronous part of code. If you need to pass this
     * into async work lambda, you should copy necessary properties.
     *
     * @return SyncAsyncResult which represents an optional synchronous work result and an optional asynchronous work
     * **/
    fun doWork(properties: PublisherProperties): SyncAsyncResult
}
