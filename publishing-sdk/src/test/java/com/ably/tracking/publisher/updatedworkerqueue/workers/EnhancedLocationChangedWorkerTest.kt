package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.test.common.anyLocation
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class EnhancedLocationChangedWorkerTest {
    private val location: Location = anyLocation()
    private val intermediateLocations = listOf(anyLocation(), anyLocation())
    private val type = LocationUpdateType.ACTUAL
    private val publisher: CorePublisher = mockk {
        every { processEnhancedLocationUpdate(any(), any(), any()) } just runs
        every { updateLocations(any()) } just runs
        every { checkThreshold(any(), any(), any()) } just runs
    }

    private val worker = EnhancedLocationChangedWorker(location, intermediateLocations, type, publisher, null)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()


    @Test
    fun `should process all enhanced location updates`() {
        // given
        val initialProperties = createPublisherProperties()
        val firstTrackable = Trackable("first-trackable")
        initialProperties.trackables.add(firstTrackable)
        val secondTrackable = Trackable("second-trackable")
        initialProperties.trackables.add(secondTrackable)

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
            publisher.processEnhancedLocationUpdate(any(), initialProperties, firstTrackable.id)
            publisher.processEnhancedLocationUpdate(any(), initialProperties, secondTrackable.id)
        }
    }

    @Test
    fun `should update locations`() {
        // given
        val initialProperties = createPublisherProperties()
        val locationUpdateSlot = mockUpdateLocationsAndCaptureLocationUpdate()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        val locationUpdate = locationUpdateSlot.captured
        assertThat(locationUpdate.location).isEqualTo(location)
        assertThat(locationUpdate.intermediateLocations).isEqualTo(intermediateLocations)
        assertThat(locationUpdate.type).isEqualTo(type)
        assertThat(locationUpdate.skippedLocations).isEmpty()
    }

    @Test
    fun `should check if threshold is reached`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = 1) {
            publisher.checkThreshold(
                location,
                initialProperties.active,
                initialProperties.estimatedArrivalTimeInMilliseconds
            )
        }
    }

    private fun mockUpdateLocationsAndCaptureLocationUpdate(): CapturingSlot<EnhancedLocationUpdate> {
        val locationUpdateSlot = slot<EnhancedLocationUpdate>()
        every { publisher.updateLocations(capture(locationUpdateSlot)) } just runs
        return locationUpdateSlot
    }
}
