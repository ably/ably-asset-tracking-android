package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


internal class EventWorkerQueue(
    private val corePublisher: CorePublisher,
    private val publisherState: DefaultCorePublisher.State
) : WorkerQueue {
    private val channel = Channel<Worker>(100)
    private val scope = CoroutineScope(createSingleThreadDispatcher())

    override suspend fun enqueue(worker: Worker) {
        channel.send(worker)
    }

    override suspend fun executeWork() {
        for (worker in channel) {
            val workResult = worker.doWork(publisherState)
            //process sync work result
            workResult.syncWorkResult?.let {
                handleWorkResult(it)
            }
            //process async work if exists
            workResult.asyncWork?.let { asyncWork ->
                scope.launch {
                    val asyncWorkResult = asyncWork()
                    handleWorkResult(asyncWorkResult)
                }
            }
        }
    }

    private suspend fun handleWorkResult(workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(workResult)
        val nextWorker = resultHandler.handle(workResult, corePublisher)
        nextWorker?.let { enqueue(it) }
    }

    override suspend fun stop() {
        channel.close()
    }
}
