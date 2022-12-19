package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.test.common.createLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class RawLocationChangedWorkerTest {
    private val publisher: CorePublisher = mockk()
    private val location: Location = createLocation()

    private val worker = RawLocationChangedWorker(location, publisher, null)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()


    @Test
    fun `should set the last publisher known location`() = runBlockingTest {
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
        assertThat(updatedProperties.lastPublisherLocation).isEqualTo(location)
    }

    @Test
    fun `should process all raw location updates if raw locations sending is enabled`() {
        // given
        val initialProperties = createPublisherProperties(areRawLocationsEnabled = true)

        val firstTrackable = Trackable("first-trackable")
        initialProperties.trackables.add(firstTrackable)
        val secondTrackable = Trackable("second-trackable")
        initialProperties.trackables.add(secondTrackable)

        every { publisher.processRawLocationUpdate(any(), any(), any()) } just runs

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
            publisher.processRawLocationUpdate(any(), updatedProperties, firstTrackable.id)
            publisher.processRawLocationUpdate(any(), updatedProperties, secondTrackable.id)
        }
    }

    @Test
    fun `should not process any raw location update if raw locations sending is disabled`() {
        // given
        val initialProperties = createPublisherProperties(areRawLocationsEnabled = false)

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

        verify(exactly = 0) {
            publisher.processRawLocationUpdate(any(), any(), any())
        }
    }

    @Test
    fun `should call the raw location changed commands if they are present`() {
        // given
        val initialProperties = createPublisherProperties(areRawLocationsEnabled = false)

        val firstCommand = anyRawLocationChangedCommandMock()
        initialProperties.rawLocationChangedCommands.add(firstCommand)

        val secondCommand = anyRawLocationChangedCommandMock()
        initialProperties.rawLocationChangedCommands.add(secondCommand)

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
            firstCommand.invoke(any())
            secondCommand.invoke(any())
        }
    }

    @Test
    fun `should clear the raw location changed commands after they are invoked`() {
        // given
        val initialProperties = createPublisherProperties(areRawLocationsEnabled = false)

        val firstCommand = anyRawLocationChangedCommandMock()
        initialProperties.rawLocationChangedCommands.add(firstCommand)

        val secondCommand = anyRawLocationChangedCommandMock()
        initialProperties.rawLocationChangedCommands.add(secondCommand)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.rawLocationChangedCommands).isEmpty()
    }

    private fun anyRawLocationChangedCommandMock(): (PublisherProperties) -> Unit = mockk(relaxed = true)
}
