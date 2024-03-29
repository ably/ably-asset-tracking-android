package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.PublisherStoppedException
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class StopWorkerTest {
    private val resultCallbackFunction = mockk<ResultCallbackFunction<Unit>>(relaxed = true)
    private val ably: Ably = mockk {
        coEvery { close(any()) } just runs
    }

    private val publisherInteractor: PublisherInteractor = mockk {
        every { stopLocationUpdates(any()) } just runs
        every { closeMapbox() } just runs
    }

    private var worker: StopWorker = createWorker()

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should stop location updates if is currently tracking trackables`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.isTracking = true

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.stopLocationUpdates(any())
        }
    }

    @Test
    fun `should not stop location updates if is not currently tracking trackables`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.isTracking = false

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.stopLocationUpdates(any())
        }
    }

    @Test
    fun `should close the whole Mapbox`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.closeMapbox()
        }
    }

    @Test
    fun `should close the whole Ably object`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        coVerify(exactly = 1) {
            ably.close(any())
        }
    }

    @Test
    fun `should dispose the publisher properties if closing Ably was successful`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(initialProperties.isDisposed)
            .isTrue()
    }

    @Test
    fun `should mark that the publisher is stopped if closing Ably was successful`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.state)
            .isEqualTo(PublisherState.STOPPED)
    }

    @Test
    fun `should call the callback function with a success if closing Ably was successful`() {
        // given
        val initialProperties = createPublisherProperties()
        val resultSlot = captureCallbackFunctionResult()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(resultSlot.captured.isSuccess)
            .isTrue()
    }

    @Test
    fun `should call the callback function with a success if publisher is already stopped`() {
        // given
        val resultSlot = captureCallbackFunctionResult()

        // when
        worker.doWhenStopped(PublisherStoppedException())

        // then
        assertThat(resultSlot.captured.isSuccess)
            .isTrue()
    }

    private fun createWorker() =
        StopWorker(resultCallbackFunction, ably, publisherInteractor)

    private fun captureCallbackFunctionResult(): CapturingSlot<Result<Unit>> {
        val resultSlot = slot<Result<Unit>>()
        every { resultCallbackFunction.invoke(capture(resultSlot)) } just runs
        return resultSlot
    }
}
