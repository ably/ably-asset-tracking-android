package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class EnterPresenceSuccessWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val publisherInteractor: PublisherInteractor = mockk {
        every { updateTrackableState(any(), any()) } just runs
        every { resolveResolution(any(), any()) } just runs
    }

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    private var worker = EnterPresenceSuccessWorker(trackable, publisherInteractor)

    @Test
    fun `it returns properties untouched if no trackable is set`() {
        // Given
        val initialProperties = createPublisherProperties()

        // When
        val updatedProperties = worker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // Then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableEnteredPresenceFlags[trackable.id]).isNull()
    }

    @Test
    fun `it does not update trackable states in publisher if no trackable is set`() {
        // Given
        val initialProperties = createPublisherProperties()

        // When
        worker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // Then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.updateTrackableState(initialProperties, trackable.id)
        }
    }

    @Test
    fun `it sets trackable presence flag if trackable set`() {
        // Given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)

        // When
        val updatedProperties = worker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // Then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableEnteredPresenceFlags[trackable.id]).isTrue()
    }

    @Test
    fun `it updates trackable states in publisher if trackable set`() {
        // Given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)

        // When
        worker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // Then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.updateTrackableState(initialProperties, trackable.id)
        }
    }

    @Test
    fun `should calculate a resolution for the added trackable when executing normally`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)

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
            publisherInteractor.resolveResolution(trackable, any())
        }
    }
}
