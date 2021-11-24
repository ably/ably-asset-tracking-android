package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.resulthandlers.getWorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * An implementation of [WorkerQueue] that uses a coroutine channel to maintain a blocking queue.
 * Even if the channel is blocking, this class is careful not to block long running operations thanks to the [Worker]
 * interface providing a way to execute both synchronous work and providing asynchronus work to be executed later.
 * */

internal class EventWorkerQueue(
    private val corePublisher: CorePublisher,
    private val publisherState: DefaultCorePublisher.Properties
) : WorkerQueue {
    /**
     * Channel to be used to send [Worker]s to, This channel is given a buffer so that it can still receive Workers
     * to be processed later in case of any sync work is blocking it
     * */
    private val channel = Channel<Worker>(100)
    private val scope = CoroutineScope(createSingleThreadDispatcher())

    /**
     * Enqueues the work, adds it to channel
     * @param [worker] : [Worker] to be enqueued
    **/
    override suspend fun enqueue(worker: Worker) {
        channel.send(worker)
    }

    /**
     * Executes the works passed to the queue by iterating and blocking the channel.
     * For each [Worker] in the queue, the work is executed using [Worker.doWork] method. This method returns a
     * result that contains an optional [SyncAsyncResult.syncWorkResult] and [SyncAsyncResult.asyncWork] . If the
     * optional sync work result exist, it's immediately handled.
     * If the optional async work exists, It's executed in a different coroutine in order to not block the queue.
     * Then, the result of this work is handled in the same way as the sync work result.
     * */
    override suspend fun executeWork() {
        for (worker in channel) {
            val workResult = worker.doWork(publisherState)

            workResult.syncWorkResult?.let {
                handleWorkResult(it)
            }

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
