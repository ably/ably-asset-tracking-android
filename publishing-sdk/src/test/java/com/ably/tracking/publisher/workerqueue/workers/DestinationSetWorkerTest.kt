package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.publisher.PublisherProperties
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DestinationSetWorkerTest {
    private lateinit var worker: DestinationSetWorker
    private val timeProvider = mockk<TimeProvider>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        createWorker()
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should always return an empty result`() {
        // given

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should update the estimated arrival time`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.estimatedArrivalTimeInMilliseconds = any()
        }
    }

    @Test
    fun `should calculate the eta by adding route duration to the current time`() {
        // given
        mockCurrentTime(currentTimeInMilliseconds = 1000L)
        createWorker(routeDurationInMilliseconds = 500L)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.estimatedArrivalTimeInMilliseconds = 1500L
        }
    }

    private fun mockCurrentTime(currentTimeInMilliseconds: Long) {
        every { timeProvider.getCurrentTimeInMilliseconds() } returns currentTimeInMilliseconds
    }

    private fun createWorker(routeDurationInMilliseconds: Long = 0L) {
        worker = DestinationSetWorker(routeDurationInMilliseconds, timeProvider)
    }
}
