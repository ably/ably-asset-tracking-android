package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.test.common.anyLocation
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class EnhancedLocationChangedWorkerTest {
    private lateinit var worker: EnhancedLocationChangedWorker
    private val location: Location = anyLocation()
    private val intermediateLocations = listOf(anyLocation(), anyLocation())
    private val type = LocationUpdateType.ACTUAL
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = EnhancedLocationChangedWorker(location, intermediateLocations, type, corePublisher)
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should always return empty result`() {
        // given

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should process all enhanced location updates`() {
        // given
        val firstTrackable = Trackable("first-trackable")
        val secondTrackable = Trackable("second-trackable")
        mockTrackables(mutableSetOf(firstTrackable, secondTrackable))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.processEnhancedLocationUpdate(any(), publisherProperties, firstTrackable.id)
            corePublisher.processEnhancedLocationUpdate(any(), publisherProperties, secondTrackable.id)
        }
    }

    @Test
    fun `should update locations`() {
        // given
        val locationUpdateSlot = mockUpdateLocationsAndCaptureLocationUpdate()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateLocations(any())
        }

        // assert that location update is created correctly
        val locationUpdate = locationUpdateSlot.captured
        Assert.assertEquals(location, locationUpdate.location)
        Assert.assertEquals(intermediateLocations, locationUpdate.intermediateLocations)
        Assert.assertEquals(type, locationUpdate.type)
        Assert.assertTrue(locationUpdate.skippedLocations.isEmpty())
    }

    @Test
    fun `should check if threshold is reached`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.checkThreshold(
                location,
                publisherProperties.active,
                publisherProperties.estimatedArrivalTimeInMilliseconds
            )
        }
    }

    private fun mockTrackables(trackables: MutableSet<Trackable>) {
        every { publisherProperties.trackables } returns trackables
    }

    private fun mockUpdateLocationsAndCaptureLocationUpdate(): CapturingSlot<EnhancedLocationUpdate> {
        val locationUpdateSlot = slot<EnhancedLocationUpdate>()
        every { corePublisher.updateLocations(capture(locationUpdateSlot)) } just runs
        return locationUpdateSlot
    }
}
