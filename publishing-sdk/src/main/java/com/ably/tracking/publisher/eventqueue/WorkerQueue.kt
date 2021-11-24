package com.ably.tracking.publisher.eventqueue

import android.util.Log
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

    suspend fun enqueue(worker: Worker) {
        channel.send(worker)
    }

    suspend fun executeWork() {
        for (worker in channel) {
            val workResult = worker.doWork()
            //process sync work result
            workResult.syncWorkResult?.let {
                handleWorkResult(it)
            }
            //process async work if exists
            workResult.asyncWork?.let { asyncWork ->
                scope.launch {
                    val asyncWorkResult = asyncWork()
                    Log.d(TAG, "executeWork: asyncWork result $asyncWorkResult")
                    handleWorkResult(asyncWorkResult)
                }
            }
        }
    }

    private suspend fun handleWorkResult(workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(workResult)
        Log.d(TAG, "handleWorkResult: result handler $resultHandler")
        val nextWorker = resultHandler.handle(workResult, corePublisher)
        nextWorker?.let { enqueue(it.worker) }
    }

    suspend fun stop() {
        channel.close()
    }
}
