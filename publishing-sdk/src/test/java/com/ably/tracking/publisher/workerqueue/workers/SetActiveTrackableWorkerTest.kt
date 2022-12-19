package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class SetActiveTrackableWorkerTest {
    private val publisher: CorePublisher = mockk {
        every { setDestination(any(), any()) } just runs
        every { removeCurrentDestination(any()) } just runs
    }

    private val resultCallbackFunction = mockk<ResultCallbackFunction<Unit>>(relaxed = true)
    private val hooks = mockk<DefaultCorePublisher.Hooks>(relaxed = true)

    private lateinit var worker: SetActiveTrackableWorker

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should always call the callback function with a success`() {
        // given
        prepareWorkerWithNewTrackable(Trackable("trackable-id", destination = null))
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
            resultCallbackFunction.invoke(Result.success(Unit))
        }
    }

    @Test
    fun `should not replace the active trackable if it is the same as the new trackable`() {
        // given
        val trackable = Trackable("test-trackable", Destination(1.0, 2.0))
        prepareWorkerWithNewTrackable(trackable)
        val initialProperties = spyk(createPublisherProperties())
        every { initialProperties.active } returns trackable

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
            initialProperties setProperty PublisherProperties::active.name value trackable
        }
    }

    @Test
    fun `should replace the active trackable if it is different than the new trackable`() {
        // given
        val trackable = Trackable("test-trackable", Destination(1.0, 2.0))
        prepareWorkerWithNewTrackable(trackable)
        val initialProperties = spyk(createPublisherProperties())
        initialProperties.active = Trackable("some-other-trackable-id")

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.active)
            .isEqualTo(trackable)
    }

    @Test
    fun `should set destination if the active trackable is different than the new trackable and has a destination`() {
        // given
        val newTrackableDestination = Destination(1.0, 2.0)
        val trackable = Trackable("test-trackable", newTrackableDestination)
        prepareWorkerWithNewTrackable(trackable)
        val initialProperties = spyk(createPublisherProperties())
        initialProperties.active = Trackable("some-other-trackable-id")

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
            publisher.setDestination(newTrackableDestination, any())
        }
    }

    @Test
    fun `should remove the current destination if the active trackable is different than the new trackable and does not have a destination`() {
        // given
        prepareWorkerWithNewTrackable(Trackable("trackable-id", destination = null))
        val initialProperties = createPublisherProperties()
        initialProperties.active = Trackable("some-other-trackable-id")

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
            publisher.removeCurrentDestination(any())
        }
    }

    private fun prepareWorkerWithNewTrackable(trackable: Trackable) {
        worker = SetActiveTrackableWorker(trackable, resultCallbackFunction, publisher, hooks)
    }
}
