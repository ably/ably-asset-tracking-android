package com.ably.tracking.common.workerqueue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A worker queue is responsible for enqueueing [Worker]s and executing them.
 * Params:
 * Properties - the type of properties used by this worker as both input and output
 * WorkerSpecification - the type of specification used to post worker back to the queue
 *
 * copyProperties - lambda used to copy properties, introducing generic copy method to [Properties] interface caused too much typing complexity.
 */
class WorkerQueue<Properties : QueueProperties, WorkerSpecification>(
    private var properties: Properties,
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory<Properties, WorkerSpecification>,
    private val copyProperties: Properties.() -> Properties,
    private val getStoppedException: () -> Exception,
    maximumWorkerQueueCapacity: Int = 100,
) {

    private val workerChannel = Channel<Worker<Properties, WorkerSpecification>>(capacity = maximumWorkerQueueCapacity)

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

    private fun execute(worker: Worker<Properties, WorkerSpecification>) {
        properties = worker.doWork(
            properties = properties.copyProperties(),
            doAsyncWork = { asyncWork -> scope.launch { asyncWork() } },
            postWork = ::enqueue
        )
    }

    /**
     * Enqueue worker created from passed specification for execution.
     *
     * @param workerSpecification [WorkerSpecification] specification of worker to be executed.
     */
    fun enqueue(workerSpecification: WorkerSpecification) {
        val worker = workerFactory.createWorker(workerSpecification)
        scope.launch { workerChannel.send(worker) }
    }
}
