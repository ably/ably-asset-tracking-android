package com.ably.tracking.common.workerqueue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A worker queue is responsible for enqueueing [Worker]s and executing them.
 * Params:
 * P - the type of properties used by this worker as both input and output
 * S - the type of specification used to post worker back to the queue
 */
class WorkerQueue<P : Properties, S>(
    private var properties: P,
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory<P, S>,
    private val copyProperties: P.() -> P,
    private val getStoppedException: () -> Exception,
    maximumWorkerQueueCapacity: Int = 100,
) {

    private val workerChannel = Channel<Worker<P, S>>(capacity = maximumWorkerQueueCapacity)

    init {
        scope.launch { executeWorkers() }
    }

    private suspend fun executeWorkers() {
        for (worker in workerChannel) {
            if (properties.isStopped) {
                worker.doWhenStopped(getStoppedException())
            } else {
                execute(worker)
            }
        }
    }

    private fun execute(worker: Worker<P, S>) {
        properties = worker.doWork(
            properties = properties.copyProperties(),
            doAsyncWork = { asyncWork -> scope.launch { asyncWork() } },
            postWork = ::enqueue
        )
    }

    /**
     * Enqueue worker created from passed specification for execution.
     *
     * @param workerSpecification [S] specification of worker to be executed.
     */
    fun enqueue(workerSpecification: S) {
        val worker = workerFactory.createWorker(workerSpecification)
        scope.launch { workerChannel.send(worker) }
    }
}
