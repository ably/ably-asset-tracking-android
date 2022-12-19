package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.anyLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class SendEnhancedLocationSuccessWorkerTest {
    private val trackableId = "test-trackable"
    private val publisher: CorePublisher = mockk {
        every { updateTrackableState(any(), trackableId) } just runs
        every { processNextWaitingEnhancedLocationUpdate(any(), any()) } just runs
    }
    private val location = anyLocation()
    private val worker = SendEnhancedLocationSuccessWorker(location, trackableId, publisher, null)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should unmark message pending state`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.enhancedLocationsPublishingState.markMessageAsPending(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.enhancedLocationsPublishingState.hasPendingMessage(trackableId))
            .isFalse()
    }

    @Test
    fun `should set the location as the last sent location`() {
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

        assertThat(updatedProperties.lastSentEnhancedLocations[trackableId])
            .isEqualTo(location)
    }

    @Test
    fun `should clear the skipped locations`() {
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

        assertThat(updatedProperties.skippedEnhancedLocations.toList(trackableId))
            .isEmpty()
    }

    @Test
    fun `should update the trackable state`() {
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

        verify(exactly = 1) {
            publisher.updateTrackableState(updatedProperties, trackableId)
        }
    }

    @Test
    fun `should process the next waiting location update if it is available`() {
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

        verify(exactly = 1) {
            publisher.processNextWaitingEnhancedLocationUpdate(updatedProperties, trackableId)
        }
    }
}
