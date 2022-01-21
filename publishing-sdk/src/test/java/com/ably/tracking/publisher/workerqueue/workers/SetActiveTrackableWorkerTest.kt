package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SetActiveTrackableWorkerTest {
    private lateinit var worker: SetActiveTrackableWorker
    private val trackable = Trackable("test-trackable", Destination(1.0, 2.0))
    private val resultCallbackFunction = mockk<ResultCallbackFunction<Unit>>(relaxed = true)
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val hooks = mockk<DefaultCorePublisher.Hooks>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        prepareWorkerWithNewTrackable(trackable)
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
    fun `should always call the callback function with a success`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resultCallbackFunction.invoke(Result.success(Unit))
        }
    }

    @Test
    fun `should not replace the active trackable if it is the same as the new trackable`() {
        // given
        prepareWorkerWithNewTrackable(trackable)
        mockActiveTrackable(trackable)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            publisherProperties setProperty PublisherProperties::active.name value trackable
        }
    }

    @Test
    fun `should replace the active trackable if it is different than the new trackable`() {
        // given
        prepareWorkerWithNewTrackable(trackable)
        mockActiveTrackable(Trackable("some-other-trackable-id"))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties setProperty PublisherProperties::active.name value trackable
        }
    }

    @Test
    fun `should set destination if the active trackable is different than the new trackable and has a destination`() {
        // given
        val newTrackableDestination = Destination(1.0, 2.0)
        prepareWorkerWithNewTrackable(Trackable("trackable-id", newTrackableDestination))
        mockActiveTrackable(Trackable("some-other-trackable-id"))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.setDestination(newTrackableDestination, any())
        }
    }

    @Test
    fun `should remove the current destination if the active trackable is different than the new trackable and does not have a destination`() {
        // given
        prepareWorkerWithNewTrackable(Trackable("trackable-id", destination = null))
        mockActiveTrackable(Trackable("some-other-trackable-id"))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.removeCurrentDestination(any())
        }
    }

    private fun mockActiveTrackable(trackable: Trackable) {
        every { publisherProperties.active } returns trackable
    }

    private fun prepareWorkerWithNewTrackable(trackable: Trackable) {
        worker = SetActiveTrackableWorker(trackable, resultCallbackFunction, corePublisher, hooks)
    }
}
