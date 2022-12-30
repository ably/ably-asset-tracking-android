package com.ably.tracking.common.workerqueue

import com.ably.tracking.common.ResultCallbackFunction

/**
 * A [Worker] interface represents workers which execute work inside [WorkerQueue].
 * Params:
 * PropertiesType - the type of properties used by this worker as both input and output
 * WorkerSpecificationType - the type of specification used to post another worker back to the queue
 */
interface Worker<PropertiesType : Properties, WorkerSpecificationType> {
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
        properties: PropertiesType,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecificationType) -> Unit
    ): PropertiesType

    /**
     * This function is provided in order for implementors to define what should happen when the worker
     * cannot [doWork] because the queue was stopped and no workers should be executed.
     * This should usually be a call to the worker's callback function with a failure with the [exception].
     *
     * @param exception The exception created by the stopped worker queue.
     */
    fun doWhenStopped(exception: Exception)

    /**
     * This function is provided in order for implementors to define what should happen when the worker
     * breaks due to an unexpected exception while [doWork] or [doWhenStopped] is being executed.
     * This should usually be a rollback operation and/or a call to the worker's callback function
     * with a failure with the [exception].
     *
     * @param exception The unexpected exception that broke the worker.
     * @param postWork this function allows worker to add other workers to the queue calling it.
     */
    fun onUnexpectedError(
        exception: Exception,
        postWork: (WorkerSpecificationType) -> Unit,
    )

    /**
     * This function is provided in order for implementors to define what should happen when the worker
     * breaks due to an unexpected exception while the async work from [doWork] is being executed.
     * This should usually be a rollback operation and/or a call to the worker's callback function
     * with a failure with the [exception].
     *
     * @param exception The unexpected exception that broke the worker.
     * @param postWork this function allows worker to add other workers to the queue calling it.
     */
    fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecificationType) -> Unit,
    )
}

/**
 * An abstract class to avoid duplication of default [doWhenStopped] implementation
 */
abstract class CallbackWorker<PropertiesType : Properties, WorkerSpecification>(protected val callbackFunction: ResultCallbackFunction<Unit>) :
    Worker<PropertiesType, WorkerSpecification> {

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
