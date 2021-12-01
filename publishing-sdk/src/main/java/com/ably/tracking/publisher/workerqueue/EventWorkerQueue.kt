package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * An implementation of [WorkerQueue] that executes incoming workers defined in [executeWorks] method. Please note
 * that this is currently an acting bridge between the older event queue (CorePublisher) . All methods must be called
 * from the corresponding channel receivers.
 * */
internal class EventWorkerQueue(
    private val corePublisher: CorePublisher,
    private val publisherProperties: DefaultCorePublisher.Properties,
    private val scope: CoroutineScope
) : WorkerQueue {

    /**
     * Executes the work in [worker] using [Worker.doWork] method, which returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork]. If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     * */
    override fun execute(worker: Worker) {
        val workResult = worker.doWork(publisherProperties)
        workResult.syncWorkResult?.let {
            handleWorkResult(it)
        }

        workResult.asyncWork?.let { asyncWork ->
            scope.launch {
                val asyncWorkResult = asyncWork()
                handleWorkResult(asyncWorkResult)
            }
        }
    }

    private fun handleWorkResult(workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(workResult)
        val nextWorker = resultHandler.handle(workResult, corePublisher)
        nextWorker?.let { corePublisher.request(it.event) }
    }
}
