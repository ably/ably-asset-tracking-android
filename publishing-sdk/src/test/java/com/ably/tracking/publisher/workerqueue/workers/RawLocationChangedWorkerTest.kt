package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.test.common.anyLocation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RawLocationChangedWorkerTest {
    private lateinit var worker: RawLocationChangedWorker
    private val location: Location = anyLocation()
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = RawLocationChangedWorker(location, corePublisher)
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
    fun `should set the last publisher known location`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.lastPublisherLocation = location
        }
    }

    @Test
    fun `should process all raw location updates if raw locations sending is enabled`() {
        // given
        val firstTrackable = Trackable("first-trackable")
        val secondTrackable = Trackable("second-trackable")
        mockRawLocationsEnabled()
        mockTrackables(mutableSetOf(firstTrackable, secondTrackable))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.processRawLocationUpdate(any(), publisherProperties, firstTrackable.id)
            corePublisher.processRawLocationUpdate(any(), publisherProperties, secondTrackable.id)
        }
    }

    @Test
    fun `should not process any raw location update if raw locations sending is disabled`() {
        // given
        val firstTrackable = Trackable("first-trackable")
        val secondTrackable = Trackable("second-trackable")
        mockRawLocationsDisabled()
        mockTrackables(mutableSetOf(firstTrackable, secondTrackable))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.processRawLocationUpdate(any(), publisherProperties, firstTrackable.id)
            corePublisher.processRawLocationUpdate(any(), publisherProperties, secondTrackable.id)
        }
    }

    @Test
    fun `should call the raw location changed commands if they are present`() {
        // given
        val firstCommand = anyRawLocationChangedCommandMock()
        val secondCommand = anyRawLocationChangedCommandMock()
        mockRawLocationChangedCommands(mutableListOf(firstCommand, secondCommand))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            firstCommand.invoke(any())
            secondCommand.invoke(any())
        }
    }

    @Test
    fun `should clear the raw location changed commands after they are invoked`() {
        // given
        mockRawLocationChangedCommands(mutableListOf(anyRawLocationChangedCommand(), anyRawLocationChangedCommand()))
        Assert.assertTrue(publisherProperties.rawLocationChangedCommands.isNotEmpty())

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(publisherProperties.rawLocationChangedCommands.isEmpty())
    }

    private fun mockRawLocationChangedCommands(commands: MutableList<(PublisherProperties) -> Unit>) {
        every { publisherProperties.rawLocationChangedCommands } returns commands
    }

    private fun mockTrackables(trackables: MutableSet<Trackable>) {
        every { publisherProperties.trackables } returns trackables
    }

    private fun mockRawLocationsEnabled() {
        every { publisherProperties.areRawLocationsEnabled } returns true
    }

    private fun mockRawLocationsDisabled() {
        every { publisherProperties.areRawLocationsEnabled } returns false
    }

    private fun anyRawLocationChangedCommand(): (PublisherProperties) -> Unit = {}

    private fun anyRawLocationChangedCommandMock(): (PublisherProperties) -> Unit = mockk(relaxed = true)
}
