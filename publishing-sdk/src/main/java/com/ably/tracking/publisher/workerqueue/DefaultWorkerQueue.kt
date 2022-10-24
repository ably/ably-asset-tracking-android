package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.PublisherStoppedException
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * The default implementation of [WorkerQueue] that [execute]s [enqueue]d workers.
 */
internal class DefaultWorkerQueue(
    private val publisherProperties: DefaultCorePublisher.Properties,
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory,
    maximumWorkerQueueCapacity: Int = 100,
) : WorkerQueue {
    private val workerChannel = Channel<Worker>(capacity = maximumWorkerQueueCapacity)

    init {
        scope.launch { executeWorkers() }
    }

    private suspend fun executeWorkers() {
        for (worker in workerChannel) {
            if (publisherProperties.state == PublisherState.STOPPED) {
                worker.doWhenStopped(PublisherStoppedException())
            } else {
                execute(worker)
            }
        }
    }

    override fun enqueue(worker: Worker) {
        scope.launch { workerChannel.send(worker) }
    }

    /**
     * Executes the work in [worker] using [Worker.doWork] method, which returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork]. If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     */
    private fun execute(worker: Worker) {
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
        val resultHandler = getWorkResultHandler(workResult, workerFactory, this)
        val nextWorker = resultHandler.handle(workResult)
        nextWorker?.let { enqueue(it) }
    }
}
