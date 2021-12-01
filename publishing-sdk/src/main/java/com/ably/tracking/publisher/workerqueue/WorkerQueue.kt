package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.workerqueue.workers.Worker

/**
 * An interface for a  worker queue. A worker queue is responsible for enqueing [Worker]s, and executing them. See
 * implementors for the implementation details, For example [EventWorkerQueue].
 *
 * Please note that this interface is likely change on the later phase of refactorings of event queue
 * **/
internal interface WorkerQueue {
    /**
     * Enqueue a worker that will immediately executes its' work
     * @param worker: [Worker] to be executed
     *
     * Please note that this behaviour is temporary and will change after refactoring.
     * **/
    fun execute(worker: Worker)
}
