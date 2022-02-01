package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Location
import com.ably.tracking.publisher.CorePublisher
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

class SendRawLocationSuccessWorkerTest {
    private lateinit var worker: SendRawLocationSuccessWorker
    private val location = anyLocation()
    private val trackableId = "test-trackable"
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val lastSentRawLocations = mockk<MutableMap<String, Location>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = SendRawLocationSuccessWorker(location, trackableId, corePublisher)
        every { publisherProperties.lastSentRawLocations } returns lastSentRawLocations
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
    fun `should unmark message pending state`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.rawLocationsPublishingState.unmarkMessageAsPending(trackableId)
        }
    }

    @Test
    fun `should set the location as the last sent location`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            lastSentRawLocations[trackableId] = location
        }
    }

    @Test
    fun `should clear the skipped locations`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.skippedRawLocations.clear(trackableId)
        }
    }

    @Test
    fun `should process the next waiting location update if it is available`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.processNextWaitingRawLocationUpdate(publisherProperties, trackableId)
        }
    }
}
