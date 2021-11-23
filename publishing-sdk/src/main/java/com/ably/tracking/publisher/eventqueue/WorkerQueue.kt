package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val TAG = "WorkerQueue"

//temporarily put core publisher for bridging
internal class WorkerQueue(private val corePublisher: CorePublisher) {
    private val channel = Channel<Worker>(100)
    private val scope = CoroutineScope(createSingleThreadDispatcher())
    private val activeWorkers = hashMapOf<Worker, MutableList<ResultCallbackFunction<*>>>()

    suspend fun enqueue(worker: Worker, resultCallbackFunctions: List<ResultCallbackFunction<*>>?) {
        activeWorkers[worker]?.let { functions ->
            resultCallbackFunctions?.let { functions.addAll(it) }
            return
        }
        channel.send(worker)
    }

    suspend fun executeWork() {
        for (worker in channel) {
            val workResult = worker.doWork()
            //process sync work result
            workResult.syncWorkResult?.let {
                handleWorkResult(worker, it)
            }
            //process async work if exists
            workResult.asyncWork?.let {asyncWork->
                scope.launch {
                    val asyncWorkResult = asyncWork()
                    handleWorkResult(worker, asyncWorkResult)
                }
            }
        }
    }

    private suspend fun handleWorkResult(worker: Worker, workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(worker)
        val nextWorker = resultHandler.handle(workResult, activeWorkers[worker], corePublisher)
        nextWorker?.let { enqueue(it.worker, it.resultCallbackFunctions) }
    }

    suspend fun stop() {
        channel.close()
    }
}
