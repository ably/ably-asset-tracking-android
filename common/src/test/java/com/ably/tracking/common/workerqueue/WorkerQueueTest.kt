package com.ably.tracking.common.workerqueue

import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.test.common.waitForCapture
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Before
import org.junit.Test

typealias TestWorkerSpecificationType = Unit

class WorkerQueueTest {
    private lateinit var workerQueue: WorkerQueue<Properties, TestWorkerSpecificationType>
    private val properties = mockk<Properties>()
    private val scope = CoroutineScope(createSingleThreadDispatcher() + SupervisorJob())
    private val workerFactory = mockk<WorkerFactory<Properties, TestWorkerSpecificationType>>()
    private val worker = mockk<Worker<Properties, TestWorkerSpecificationType>>(relaxed = true)

    @Before
    fun setup() {
        mockAllWorkers(worker)
        workerQueue = WorkerQueue(
            properties = properties,
            scope = scope,
            workerFactory = workerFactory,
            copyProperties = { properties },
            getStoppedException = { Exception("WorkerQueue is stopped") }
        )
    }

    @After
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `started queue should call worker's regular work method`() {
        // given
        mockWorkerQueueStarted()

        // when
        workerQueue.enqueue(Unit)

        // then
        verify(exactly = 1) { worker.doWork(any(), any(), any()) }
    }

    @Test
    fun `stopped queue should call worker's on stopped method`() {
        // given
        mockWorkerQueueStopped()

        // when
        workerQueue.enqueue(Unit)

        // then
        verify(exactly = 1) { worker.doWhenStopped(any()) }
    }

    @Test
    fun `when an unexpected exception is thrown by worker's regular work method, the queue should call worker's unexpected exception method`() {
        // given
        mockWorkerQueueStarted()
        every { worker.doWork(any(), any(), any()) } throws anyUnexpectedException()

        // when
        workerQueue.enqueue(Unit)

        // then
        verify(exactly = 1) { worker.onUnexpectedError(any(), any()) }
    }

    @Test
    fun `when an unexpected exception is thrown by worker's async work, the queue should call worker's unexpected async exception method`() {
        // given
        mockWorkerQueueStarted()
        val asyncWorkSlot: CapturingSlot<(suspend () -> Unit) -> Unit> = slot()
        every { worker.doWork(any(), capture(asyncWorkSlot), any()) } returns properties

        // when
        workerQueue.enqueue(Unit)
        asyncWorkSlot.waitForCapture()
        asyncWorkSlot.captured { throw anyUnexpectedException() }

        // then
        verify(exactly = 1) { worker.onUnexpectedAsyncError(any(), any()) }
    }

    @Test
    fun `when an unexpected exception is thrown by worker's on stopped method, the queue should call worker's unexpected exception method`() {
        // given
        mockWorkerQueueStopped()
        every { worker.doWhenStopped(any()) } throws anyUnexpectedException()

        // when
        workerQueue.enqueue(Unit)

        // then
        verify(exactly = 1) { worker.onUnexpectedError(any(), any()) }
    }

    private fun mockWorkerQueueStopped() {
        every { properties.isStopped } returns true
    }

    private fun mockWorkerQueueStarted() {
        every { properties.isStopped } returns false
    }

    private fun mockAllWorkers(worker: Worker<Properties, TestWorkerSpecificationType>) {
        every { workerFactory.createWorker(Unit) } returns worker
    }

    private fun anyUnexpectedException() = java.lang.IllegalStateException("Unexpected worker exception")
}
