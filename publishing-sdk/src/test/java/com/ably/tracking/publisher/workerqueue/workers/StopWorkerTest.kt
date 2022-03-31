package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.test.common.mockCloseFailure
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
        worker = StopWorker(resultCallbackFunction, ably, corePublisher)
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
            publisherProperties setProperty PublisherProperties::isStopped.name value true
        }
    }

    @Test
    fun `should not mark that the publisher is stopped if closing Ably failed`() {
        // given
        ably.mockCloseFailure()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            publisherProperties setProperty PublisherProperties::isStopped.name value true
        }
    }

    @Test
    fun `should call the callback function with a success if closing Ably was successful`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resultCallbackFunction.invoke(Result.success(Unit))
        }
    }

    @Test
    fun `should call the callback function with a failure if closing Ably failed`() {
        // given
        val closeAblyException = ConnectionException(ErrorInformation(""))
        ably.mockCloseFailure(closeAblyException)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resultCallbackFunction.invoke(Result.failure(closeAblyException))
        }
    }
}
