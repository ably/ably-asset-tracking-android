package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

/**
 * This interface is intended for handling [WorkResult]s that are received from  [Worker]s synchronous or asynchronus
 * work.
 *
 * @param T: Type of [WorkResult] to be handled by implementors
 * **/
internal interface WorkResultHandler<in T : WorkResult> {
    /**
     * Handles [WorkResult] given to it.
     *
     * @param workResult : [WorkResult] to be handled
     *
     * @return an optional [Worker] if implementors decide there is a need to add another worker to the queue.
     * **/
    fun handle(workResult: T): Worker?
}
