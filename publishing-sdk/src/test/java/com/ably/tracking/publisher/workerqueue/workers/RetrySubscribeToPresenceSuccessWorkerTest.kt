package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal class RetrySubscribeToPresenceSuccessWorkerTest {
    lateinit var worker: RetrySubscribeToPresenceSuccessWorker
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val trackable = Trackable("test-trackable")
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val trackableSubscribedToPresenceFlags: MutableMap<String, Boolean> = mockk(relaxed = true)

    @Before
    fun setUp() {
        worker = RetrySubscribeToPresenceSuccessWorker(trackable, corePublisher)
        mockTrackableIsAdded()
        every { publisherProperties.trackableSubscribedToPresenceFlags } returns trackableSubscribedToPresenceFlags
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
    fun `should not try to update trackable state if the trackable is not in the added trackable set`() {
        // given
        mockTrackableIsNotAdded()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            trackableSubscribedToPresenceFlags[trackable.id] = true
            corePublisher.updateTrackableState(any(), trackable.id)
        }
    }

    @Test
    fun `should set the subscribed to presence flag to true and then update trackable states`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verifyOrder {
            trackableSubscribedToPresenceFlags[trackable.id] = true
            corePublisher.updateTrackableState(any(), trackable.id)
        }
    }

    private fun mockTrackableIsAdded() {
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
    }

    private fun mockTrackableIsNotAdded() {
        every { publisherProperties.trackables } returns mutableSetOf()
    }
}
