package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ChannelConnectionStateChangeWorkerTest {
    private lateinit var worker: ChannelConnectionStateChangeWorker
    private val connectionStateChange = ConnectionStateChange(ConnectionState.OFFLINE, null)
    private val trackableId = "test-trackable"
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val lastChannelConnectionStateChanges = mockk<MutableMap<String, ConnectionStateChange>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = ChannelConnectionStateChangeWorker(connectionStateChange, trackableId, corePublisher)
        every { publisherProperties.lastChannelConnectionStateChanges } returns lastChannelConnectionStateChanges
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
    fun `should set the last channel connection state change for the given trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            lastChannelConnectionStateChanges[trackableId] = connectionStateChange
        }
    }

    @Test
    fun `should update trackable state for the given trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateTrackableState(publisherProperties, trackableId)
        }
    }
}
