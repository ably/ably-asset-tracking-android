package com.ably.tracking.publisher.eventqueue

import android.util.Log
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "WorkerQueue"

//temporarily put core publisher for bridging
internal class WorkerQueue(private val corePublisher: CorePublisher) {
    private val channel = Channel<Worker>(100)
    private val scope = CoroutineScope(createSingleThreadDispatcher())
    private val callbackFunctions = hashMapOf<Worker, ResultCallbackFunction<StateFlow<TrackableState>>>()


    suspend fun enqueue(worker: Worker, callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>) {
        callbackFunctions[worker]?.let { functions ->
            //do not handle this yet
            return
        }
        callbackFunction?.let {
            callbackFunctions.put(worker, it)
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
            workResult.asyncWork?.let { asyncWork ->
                scope.launch {
                    val asyncWorkResult = asyncWork()
                    Log.d(TAG, "executeWork: asyncWork result $asyncWorkResult")
                    handleWorkResult(worker, asyncWorkResult)
                }
            }
        }
    }

    private suspend fun handleWorkResult(worker: Worker, workResult: WorkResult) {
        val resultHandler = getWorkResultHandler(workResult)
        Log.d(TAG, "handleWorkResult: result handler $resultHandler")
        val nextWorker = resultHandler.handle(workResult, callbackFunctions[worker], corePublisher)
        nextWorker?.let { enqueue(it.worker, it.resultCallbackFunction!!) }
    }

    suspend fun stop() {
        channel.close()
    }
}
