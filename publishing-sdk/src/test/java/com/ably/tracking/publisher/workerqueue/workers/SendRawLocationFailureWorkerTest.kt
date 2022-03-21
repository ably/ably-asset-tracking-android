package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.LocationsPublishingState
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.test.common.anyLocation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SendRawLocationFailureWorkerTest {
    private lateinit var worker: SendRawLocationFailureWorker
    private val locationUpdate = LocationUpdate(anyLocation(), emptyList())
    private val trackableId = "test-trackable"
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val lastSentRawLocations = mockk<MutableMap<String, Location>>(relaxed = true)
    private val rawLocationsPublishingState = mockk<LocationsPublishingState<LocationUpdate>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = SendRawLocationFailureWorker(locationUpdate, trackableId, null, corePublisher, null)
        every { publisherProperties.lastSentRawLocations } returns lastSentRawLocations
        every { publisherProperties.rawLocationsPublishingState } returns rawLocationsPublishingState
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
    fun `should send the location again if should retry publishing`() {
        // given
        mockShouldRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.retrySendingRawLocation(publisherProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should not send the location again if should not retry publishing`() {
        // given
        mockShouldNotRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.retrySendingRawLocation(publisherProperties, trackableId, locationUpdate)
        }
    }

    @Test
    fun `should unmark message pending state if should not retry publishing`() {
        // given
        mockShouldNotRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        }
    }

    @Test
    fun `should not unmark message pending state if should retry publishing`() {
        // given
        mockShouldRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        }
    }

    @Test
    fun `should save location for further sending if should not retry publishing`() {
        // given
        mockShouldNotRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.saveRawLocationForFurtherSending(
                publisherProperties,
                trackableId,
                locationUpdate.location
            )
        }
    }

    @Test
    fun `should not save location for further sending if should retry publishing`() {
        // given
        mockShouldRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.saveRawLocationForFurtherSending(
                publisherProperties,
                trackableId,
                locationUpdate.location
            )
        }
    }

    @Test
    fun `should process next waiting location if should not retry publishing`() {
        // given
        mockShouldNotRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.processNextWaitingRawLocationUpdate(publisherProperties, trackableId)
        }
    }

    @Test
    fun `should not process next waiting location if should retry publishing`() {
        // given
        mockShouldRetryPublishing()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.processNextWaitingRawLocationUpdate(publisherProperties, trackableId)
        }
    }

    private fun mockShouldRetryPublishing() {
        every { rawLocationsPublishingState.shouldRetryPublishing(trackableId) } returns true
    }

    private fun mockShouldNotRetryPublishing() {
        every { rawLocationsPublishingState.shouldRetryPublishing(trackableId) } returns false
    }
}
