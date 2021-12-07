package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.workerqueue.results.WorkResult

/**
 * A generic interface for workers
 *
 * @param I: Parameter to be provided which will act as an input for the worker to do its work
 *
 * @param T: [WorkResult] that is to be provided as an output by the worker
 * */
internal interface Worker<in I, out T : WorkResult> {
    /**
     * Performs the worker given to the worker
     *
     * @param input : Input for this worker to do its work.
     *
     * @return T : [WorkResult] to be returned by this worker
     * */
    fun doWork(input: I): T
}
