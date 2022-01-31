package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RoutingProfile
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ChangeRoutingProfileWorkerTest {
    private lateinit var worker: ChangeRoutingProfileWorker
    private val routingProfile = RoutingProfile.WALKING
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = ChangeRoutingProfileWorker(routingProfile, corePublisher)
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
    fun `should set the routing profile`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.routingProfile = routingProfile
        }
    }

    @Test
    fun `should refresh the destination with the new routing profile if the current destination is present`() {
        // given
        val currentDestination = Destination(1.0, 1.0)
        every { publisherProperties.currentDestination } returns currentDestination

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.setDestination(currentDestination, publisherProperties)
        }
    }

    @Test
    fun `should not refresh the destination with the new routing profile if there is no current destination`() {
        // given
        every { publisherProperties.currentDestination } returns null

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.setDestination(any(), publisherProperties)
        }
    }
}
