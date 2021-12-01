package com.ably.tracking.publisher.workerqueue

import android.util.Log
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * An implementation of [WorkerQueue] that executes incoming workers defined in [executeWork] method. Please note
 * that this is currently an acting bridge between the older event queue (CorePublisher) . All methods must be called
 * from the corresponding channel receivers.
 * */
private const val TAG = "EventWorkerQueue"

internal class EventWorkerQueue(
    private val corePublisher: CorePublisher,
    private val publisherProperties: DefaultCorePublisher.Properties,
    private val scope: CoroutineScope
) : WorkerQueue {

    /**
     * Enqueues the work, And immediately executes it.
     * @param [worker] : [Worker] to be enqueued
     * Please note that the behaviour of this class will change so it enqueues works instead of executing them
     * directly. Current behaviour is temporary and is going to change after all events have their corresponding
     * worker and we no longer use a channel in CorePublisher
     **/
    override fun enqueue(worker: Worker) {
        executeWork(worker)
    }

    /**
     * Executes the work in [worker] using [Worker.doWork] method, which returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork]. If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     * */
    private fun executeWork(worker: Worker) {
        val workResult = worker.doWork(publisherProperties)
        Log.d(TAG, "executeWork: ")
        workResult.syncWorkResult?.let {
            handleWorkResult(it)
        }

        workResult.asyncWork?.let { asyncWork ->
            scope.launch {
                Log.d(TAG, "executeWork: asyncWork ")

                val asyncWorkResult = asyncWork()
                handleWorkResult(asyncWorkResult)
            }
        }
    }

    private fun handleWorkResult(workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(workResult)
        val nextWorker = resultHandler.handle(workResult, corePublisher)
        nextWorker?.let { enqueue(it) }
    }
}
