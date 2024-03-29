package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.LocationUpdate
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

class SendRawLocationFailureWorkerTest {

    private val trackableId = "test-trackable"
    private val locationUpdate = LocationUpdate(anyLocation(), emptyList())
    private val publisherInteractor: PublisherInteractor = mockk {
        every { saveRawLocationForFurtherSending(any(), any(), any()) } just runs
        every { processNextWaitingRawLocationUpdate(any(), any()) } just runs
        every { retrySendingRawLocation(any(), any(), any()) } just runs
    }

    private val worker = SendRawLocationFailureWorker(locationUpdate, trackableId, null, publisherInteractor, null)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should send the location again if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.rawLocationsPublishingState.clearAll()

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
            publisherInteractor.retrySendingRawLocation(updatedProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should not send the location again if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.rawLocationsPublishingState.maxOutRetryCount(trackableId)

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
            publisherInteractor.retrySendingRawLocation(updatedProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should unmark message pending state if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.rawLocationsPublishingState.maxOutRetryCount(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.rawLocationsPublishingState.hasPendingMessage(trackableId))
            .isFalse()
    }

    @Test
    fun `should not unmark message pending state if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.rawLocationsPublishingState.clearAll()
        initialProperties.rawLocationsPublishingState.markMessageAsPending(trackableId)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.rawLocationsPublishingState.hasPendingMessage(trackableId))
            .isTrue()
    }

    @Test
    fun `should save location for further sending if should not retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.rawLocationsPublishingState.maxOutRetryCount(trackableId)

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
            publisherInteractor.saveRawLocationForFurtherSending(
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
        initialProperties.rawLocationsPublishingState.clearAll()

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
            publisherInteractor.saveRawLocationForFurtherSending(
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
        initialProperties.rawLocationsPublishingState.maxOutRetryCount(trackableId)

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
            publisherInteractor.processNextWaitingRawLocationUpdate(updatedProperties, trackableId)
        }
    }

    @Test
    fun `should not process next waiting location if should retry publishing`() {
        // given
        val initialProperties = createPublisherProperties()
        // set all the retry counters to 0
        initialProperties.rawLocationsPublishingState.clearAll()

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
            publisherInteractor.processNextWaitingRawLocationUpdate(updatedProperties, trackableId)
        }
    }
}
