package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
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

class SendEnhancedLocationFailureWorkerTest {
    private lateinit var worker: SendEnhancedLocationFailureWorker
    private val locationUpdate =
        EnhancedLocationUpdate(anyLocation(), emptyList(), emptyList(), LocationUpdateType.ACTUAL)
    private val trackableId = "test-trackable"
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val lastSentEnhancedLocations = mockk<MutableMap<String, Location>>(relaxed = true)
    private val enhancedLocationsPublishingState =
        mockk<LocationsPublishingState<EnhancedLocationUpdate>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = SendEnhancedLocationFailureWorker(locationUpdate, trackableId, null, corePublisher, null)
        every { publisherProperties.lastSentEnhancedLocations } returns lastSentEnhancedLocations
        every { publisherProperties.enhancedLocationsPublishingState } returns enhancedLocationsPublishingState
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
            corePublisher.retrySendingEnhancedLocation(publisherProperties, trackableId, locationUpdate)
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
            corePublisher.retrySendingEnhancedLocation(publisherProperties, trackableId, locationUpdate)
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
            enhancedLocationsPublishingState.unmarkMessageAsPending(trackableId)
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
            enhancedLocationsPublishingState.unmarkMessageAsPending(trackableId)
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
            corePublisher.saveEnhancedLocationForFurtherSending(
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
            corePublisher.saveEnhancedLocationForFurtherSending(
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
            corePublisher.processNextWaitingEnhancedLocationUpdate(publisherProperties, trackableId)
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
            corePublisher.processNextWaitingEnhancedLocationUpdate(publisherProperties, trackableId)
        }
    }

    private fun mockShouldRetryPublishing() {
        every { enhancedLocationsPublishingState.shouldRetryPublishing(trackableId) } returns true
    }

    private fun mockShouldNotRetryPublishing() {
        every { enhancedLocationsPublishingState.shouldRetryPublishing(trackableId) } returns false
    }
}
