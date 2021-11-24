package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.workerqueue.workers.Worker

/**
 * An interface for a coroutine based worker queue. A worker queue is responibly for queing [Worker]s, executing them
 * and stopping the queue from accepting any more works. All these operations will have to be invoked from a
 * coroutine scope. See implementors for the implementation details, For example [EventWorkerQueue]
 * **/
internal interface WorkerQueue {
    /**
     * Enqueue a worker so it is scheduled to do its work when it is 'dequeue'd. Implementors must be aware   that this
     * method could be invoked from any coroutine. They must make sure that enqueing work is fair. [EventWorkerQueue]
     * uses coroutine channels to achieve this.
     * @param [worker] : [Worker] to be enqueued
     * **/
    suspend fun enqueue(worker: Worker)
    /**
     * Start processing the worker queue. This operation will open the queue for executing so that whenever a new
     * work is available it must process it. Even if there are no works, the queue should still be available for work
     * processsing
     * **/
    suspend fun executeWork()
    /**
     * This method will stop queue from excepting any more workers. It will also stop processing of (if any)
     * remaining workers.
     * */
    suspend fun stop()
}
