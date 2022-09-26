package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties

/**
 * A [Worker] interface represents workers which executes synchronous work with an optional synchronous work result
 * and another optional asynchronous work.
 */
internal interface Worker {
    /**
     * This function is provided in order for implementors to implement synchronous work. They may optionally
     * provide result for the executed synchronous work and an optional asynchronous work in the form of a suspending
     * function.
     *
     * @param properties current state of publisher to be used by this worker.
     *
     * Please use [properties] directly only at synchronous part of code. If you need to pass this
     * into async work lambda, you should copy necessary properties.
     *
     * @return SyncAsyncResult which represents an optional synchronous work result and an optional asynchronous work
     */
    fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    )

    /**
     * This function is provided in order for implementors to define what should happen when the worker
     * cannot [doWork] because the queue was stopped and no workers should be executed.
     * This should usually be a call to the worker's callback function with a failure with the [exception].
     *
     * @param exception The exception created by the stopped worker queue.
     */
    fun doWhenStopped(exception: Exception)
}

internal abstract class CallbackWorker(protected val callbackFunction: ResultCallbackFunction<Unit>) :
    Worker {

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
