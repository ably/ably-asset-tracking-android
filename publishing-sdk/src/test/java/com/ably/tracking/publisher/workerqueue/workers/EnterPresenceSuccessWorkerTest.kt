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
    }

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    private lateinit var worker: EnterPresenceSuccessWorker

    @Test
    fun `it returns properties untouched if no trackable is set`() {
        // Given
        worker = EnterPresenceSuccessWorker(trackable, publisherInteractor)
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
        worker = EnterPresenceSuccessWorker(trackable, publisherInteractor)
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
        worker = EnterPresenceSuccessWorker(trackable, publisherInteractor)
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
        worker = EnterPresenceSuccessWorker(trackable, publisherInteractor)
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
}
