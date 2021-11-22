package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.common.createSingleThreadDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class WorkerQueue {
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
                    println("asynWorkResult $asyncWorkResult")
                }
            }
        }
    }

    suspend fun stop() {
        channel.close()
    }
}
