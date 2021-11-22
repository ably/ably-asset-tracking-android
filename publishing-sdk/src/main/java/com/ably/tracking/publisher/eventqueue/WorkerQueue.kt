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
    val channel = Channel<Worker>(100)
    val scope = CoroutineScope(createSingleThreadDispatcher())

    suspend fun enqueue(worker: Worker) {
        channel.send(worker)
    }

    suspend fun executeWork() {
        for (worker in channel) {
            val asyncWork = worker.doWork()
            asyncWork?.let {
                scope.launch {
                    val asyncWorkResult = it()
                    val handler = ResultHandlers.handlerFor(worker)
                    handler.handle(asyncWorkResult,corePublisher)
                    stop()
                }
            }
        }
    }

    suspend fun stop() {
        channel.close()
    }
}
