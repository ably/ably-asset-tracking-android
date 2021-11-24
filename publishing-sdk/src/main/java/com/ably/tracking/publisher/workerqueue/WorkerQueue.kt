package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.workerqueue.workers.Worker

internal interface WorkerQueue {
    suspend fun enqueue(worker: Worker)
    suspend fun executeWork()
    suspend fun stop()
}
