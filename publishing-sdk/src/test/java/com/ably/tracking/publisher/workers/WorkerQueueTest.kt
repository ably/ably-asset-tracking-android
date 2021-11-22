package com.ably.tracking.publisher.workers

import com.ably.tracking.publisher.eventqueue.AddTrackableWorker
import com.ably.tracking.publisher.eventqueue.WorkerQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class WorkerQueueTest {
    private val queue = WorkerQueue()

    @Test
    fun executeWorkers() = runBlocking {
        for (i in 1..20) {
            queue.enqueue(AddTrackableWorker(((20 - i) * 10).toLong()))
        }
        launch {
            queue.executeWork()
        }
        delay(4000)

        queue.stop()
    }
}
