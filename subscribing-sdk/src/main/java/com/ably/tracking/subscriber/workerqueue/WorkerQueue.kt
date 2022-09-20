package com.ably.tracking.subscriber.workerqueue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A worker queue is responsible for enqueueing [Worker]s and executing them.
 */
internal class WorkerQueue(
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory,
    maximumWorkerQueueCapacity: Int = 100,
) {

    private val workerChannel = Channel<Worker>(capacity = maximumWorkerQueueCapacity)

    init {
        scope.launch { executeWorkers() }
    }

    /**
     * Executes the work in [worker] using [Worker.doWork] method, which returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork]. If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     */
    private suspend fun executeWorkers() {
        for (worker in workerChannel) {
//            if (publisherProperties.isStopped) {
//                worker.doWhenStopped(PublisherStoppedException())
//            } else {
            execute(worker)
//            }
        }
    }

    private fun execute(worker: Worker) {
        worker.doWork(
            properties = worker,
            doAsyncWork = { block -> scope.launch { block() } },
            postWork = ::enqueue
        )
    }

    /**
     * Enqueue worker created from passed params for execution.
     *
     * @param worker [Worker] to be executed.
     */
    fun enqueue(workerParams: WorkerParams) {
        val worker = workerFactory.createWorker(workerParams)
        scope.launch { workerChannel.send(worker) }
    }

    /**
     * Enqueue a worker for execution.
     *
     * @param worker [Worker] to be executed.
     */
    fun enqueue(worker: Worker) {
        scope.launch { workerChannel.send(worker) }
    }
}
