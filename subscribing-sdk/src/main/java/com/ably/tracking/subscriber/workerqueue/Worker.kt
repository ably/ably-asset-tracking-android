package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties

/**
 * A [Worker] interface represents workers which execute work inside [WorkerQueue].
 */
internal interface Worker {
    /**
     * This function is provided in order for implementors to implement synchronous work. Any asynchronous tasks
     * should be executed inside [doAsyncWork] function. If a worker needs to delegate another task to the queue
     * pass it to [postWork] function.
     *
     * @param properties copy of current state of publisher to be used by this worker.
     * @param doAsyncWork wrapper function for asynchronous work.
     * @param postWork this function allows worker to add other workers to the queue calling it.
     *
     * @return updated [Properties] modified by this worker.
     */
    fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): Properties

    /**
     * This function is provided in order for implementors to define what should happen when the worker
     * cannot [doWork] because the queue was stopped and no workers should be executed.
     * This should usually be a call to the worker's callback function with a failure with the [exception].
     *
     * @param exception The exception created by the stopped worker queue.
     */
    fun doWhenStopped(exception: Exception)
}

/**
 * An abstract class to avoid duplication of default [doWhenStopped] implementation
 */
internal abstract class CallbackWorker(protected val callbackFunction: ResultCallbackFunction<Unit>) :
    Worker {

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
