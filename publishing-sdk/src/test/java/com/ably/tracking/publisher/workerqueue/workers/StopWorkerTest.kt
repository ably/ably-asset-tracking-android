package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.test.common.mockCloseFailure
import com.ably.tracking.test.common.mockCloseSuccessWithDelay
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class StopWorkerTest {
    private lateinit var worker: StopWorker
    private val resultCallbackFunction = mockk<ResultCallbackFunction<Unit>>(relaxed = true)
    private val ably = mockk<Ably>(relaxed = true)
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = createWorker()
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
    fun `should stop location updates if is currently tracking trackables`() {
        // given
        every { publisherProperties.isTracking } returns true

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.stopLocationUpdates(any())
        }
    }

    @Test
    fun `should not stop location updates if is not currently tracking trackables`() {
        // given
        every { publisherProperties.isTracking } returns false

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.stopLocationUpdates(any())
        }
    }

    @Test
    fun `should close the whole Mapbox`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.closeMapbox()
        }
    }

    @Test
    fun `should close the whole Ably object`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        coVerify(exactly = 1) {
            ably.close(any())
        }
    }

    @Test
    fun `should dispose the publisher properties if closing Ably was successful`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.dispose()
        }
    }

    @Test
    fun `should not dispose the publisher properties if closing Ably failed`() {
        // given
        ably.mockCloseFailure()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            publisherProperties.dispose()
        }
    }

    @Test
    fun `should mark that the publisher is stopped if closing Ably was successful`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties setProperty PublisherProperties::state.name value PublisherState.STOPPED
        }
    }

    @Test
    fun `should mark that the publisher is stopped if closing Ably failed`() {
        // given
        ably.mockCloseFailure()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties setProperty PublisherProperties::state.name value PublisherState.STOPPED
        }
    }

    @Test
    fun `should mark that the publisher is stopped if stopping thrown a timeout`() {
        // given
        worker = createWorker(timeoutInMilliseconds = 10L)
        ably.mockCloseSuccessWithDelay(delayInMilliseconds = 1000L)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties setProperty PublisherProperties::state.name value PublisherState.STOPPED
        }
    }

    @Test
    fun `should call the callback function with a success if closing Ably was successful`() {
        // given
        val resultSlot = captureCallbackFunctionResult()

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(resultSlot.captured.isSuccess)
    }

    @Test
    fun `should call the callback function with a failure if closing Ably failed`() {
        // given
        ably.mockCloseFailure()
        val resultSlot = captureCallbackFunctionResult()

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(resultSlot.captured.isFailure)
    }

    @Test
    fun `should call the callback function with a failure if stopping thrown a timeout`() {
        // given
        worker = createWorker(timeoutInMilliseconds = 10L)
        ably.mockCloseSuccessWithDelay(delayInMilliseconds = 1000L)
        val resultSlot = captureCallbackFunctionResult()

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(resultSlot.captured.isFailure)
    }

    private fun createWorker(timeoutInMilliseconds: Long = 30_000L) =
        StopWorker(resultCallbackFunction, ably, corePublisher, timeoutInMilliseconds)

    private fun captureCallbackFunctionResult(): CapturingSlot<Result<Unit>> {
        val resultSlot = slot<Result<Unit>>()
        every { resultCallbackFunction.invoke(capture(resultSlot)) } just runs
        return resultSlot
    }
}
