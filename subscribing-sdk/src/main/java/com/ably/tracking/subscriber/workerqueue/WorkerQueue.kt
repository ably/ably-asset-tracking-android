package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberStoppedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A worker queue is responsible for enqueueing [Worker]s and executing them.
 */
internal class WorkerQueue(
    private val subscriberProperties: Properties,
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory,
    maximumWorkerQueueCapacity: Int = 100,
) {

    private val workerChannel = Channel<Worker>(capacity = maximumWorkerQueueCapacity)

    init {
        scope.launch { executeWorkers() }
    }

    private suspend fun executeWorkers() {
        for (worker in workerChannel) {
            if (subscriberProperties.isStopped) {
                worker.doWhenStopped(SubscriberStoppedException())
            } else {
                execute(worker)
            }
        }
    }

    private fun execute(worker: Worker) {
        worker.doWork(
            properties = subscriberProperties,
            doAsyncWork = { block -> scope.launch { block() } },
            postWork = ::enqueue
        )
    }

    /**
     * Enqueue worker created from passed params for execution.
     *
     * @param worker [Worker] to be executed.
     */
    fun enqueue(workerSpecification: WorkerSpecification) {
        val worker = workerFactory.createWorker(workerSpecification)
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
