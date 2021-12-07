package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.AdhocEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.SyncAsyncWorker
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * An implementation of [WorkerQueue] that executes incoming workers defined in [execute] method. Please note
 * that this is currently an acting bridge between the older event queue (CorePublisher) . All methods must be called
 * from the corresponding channel receivers.
 * */
internal class EventWorkerQueue(
    private val corePublisher: CorePublisher,
    private val publisherProperties: DefaultCorePublisher.Properties,
    private val scope: CoroutineScope
) : WorkerQueue {

    /**
     * Executes the work in [worker] using [SyncAsyncWorker.doWork] method, which returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork]. If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     * */
    override fun execute(worker: SyncAsyncWorker) {
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
        nextWorker?.let {
            it.event.apply {
                when (this) {
                    is Request<*> -> corePublisher.request(this)
                    is AdhocEvent -> corePublisher.enqueue(this)
                }
            }
        }
    }
}
