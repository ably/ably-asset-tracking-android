package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class RetrySubscribeToPresenceSuccessWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val worker = RetrySubscribeToPresenceSuccessWorker(trackable, corePublisher)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should not try to update trackable state if the trackable is not in the added trackable set`() {
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

        assertThat(updatedProperties.trackableSubscribedToPresenceFlags[trackable.id]).isNull()
        verify(exactly = 0) {
            corePublisher.updateTrackableState(any(), trackable.id)
        }
    }

    @Test
    fun `should set the subscribed to presence flag to true and then update trackable states`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableSubscribedToPresenceFlags[trackable.id]).isTrue()
        verify(exactly = 1) {
            corePublisher.updateTrackableState(any(), trackable.id)
        }
    }
}
