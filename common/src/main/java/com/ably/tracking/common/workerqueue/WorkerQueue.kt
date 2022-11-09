package com.ably.tracking.common.workerqueue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A worker queue is responsible for enqueueing [Worker]s and executing them.
 * Params:
 * PropertiesType - the type of properties used by workers as both input and output
 * WorkerSpecificationType - the type of specification used to post worker back to the queue
 *
 * copyProperties - lambda used to copy properties, introducing generic copy method to [Properties] interface caused too much typing complexity.
 */
class WorkerQueue<PropertiesType : Properties, WorkerSpecificationType>(
    private var properties: PropertiesType,
    private val scope: CoroutineScope,
    private val workerFactory: WorkerFactory<PropertiesType, WorkerSpecificationType>,
    private val copyProperties: PropertiesType.() -> PropertiesType,
    private val getStoppedException: () -> Exception,
    maximumWorkerQueueCapacity: Int = 100,
) {

    private val workerChannel = Channel<Worker<PropertiesType, WorkerSpecificationType>>(capacity = maximumWorkerQueueCapacity)

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

    private fun execute(worker: Worker<PropertiesType, WorkerSpecificationType>) {
        properties = worker.doWork(
            properties = properties.copyProperties(),
            doAsyncWork = { asyncWork -> scope.launch { asyncWork() } },
            postWork = ::enqueue
        )
    }

    /**
     * Enqueue worker created from passed specification for execution.
     *
     * @param workerSpecification [WorkerSpecificationType] specification of worker to be executed.
     */
    fun enqueue(workerSpecification: WorkerSpecificationType) {
        val worker = workerFactory.createWorker(workerSpecification)
        scope.launch { workerChannel.send(worker) }
    }
}
