package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.anyLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class SendEnhancedLocationFailureWorkerTest {
    private val locationUpdate =
        EnhancedLocationUpdate(anyLocation(), emptyList(), emptyList(), LocationUpdateType.ACTUAL)
    private val trackableId = "test-trackable"
    private val publisherInteractor: PublisherInteractor = mockk {
        every { saveEnhancedLocationForFurtherSending(any(), any(), any()) } just runs
        every { retrySendingEnhancedLocation(any(), any(), any()) } just runs
        every { processNextWaitingEnhancedLocationUpdate(any(), any()) } just runs
    }
    private val worker = SendEnhancedLocationFailureWorker(locationUpdate, trackableId, null, publisherInteractor, null)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should send the location again if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.enhancedLocationsPublishingState.clearAll()

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
            publisherInteractor.retrySendingEnhancedLocation(initialProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should not send the location again if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.enhancedLocationsPublishingState.maxOutRetryCount(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.retrySendingEnhancedLocation(updatedProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should unmark message pending state if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.enhancedLocationsPublishingState.maxOutRetryCount(trackableId)

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
    fun `should not unmark message pending state if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.enhancedLocationsPublishingState.clearAll()
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
            .isTrue()
    }

    @Test
    fun `should save location for further sending if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.enhancedLocationsPublishingState.maxOutRetryCount(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = 1) {
            publisherInteractor.saveEnhancedLocationForFurtherSending(
                updatedProperties,
                trackableId,
                locationUpdate.location
            )
        }
    }

    @Test
    fun `should not save location for further sending if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.enhancedLocationsPublishingState.clearAll()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = 0) {
            publisherInteractor.saveEnhancedLocationForFurtherSending(
                updatedProperties,
                trackableId,
                locationUpdate.location
            )
        }
    }

    @Test
    fun `should process next waiting location if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.enhancedLocationsPublishingState.maxOutRetryCount(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = 1) {
            publisherInteractor.processNextWaitingEnhancedLocationUpdate(updatedProperties, trackableId)
        }
    }

    @Test
    fun `should not process next waiting location if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.enhancedLocationsPublishingState.clearAll()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = 0) {
            publisherInteractor.processNextWaitingEnhancedLocationUpdate(updatedProperties, trackableId)
        }
    }
}
