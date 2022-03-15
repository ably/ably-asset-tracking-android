package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AblyConnectionStateChangeWorkerTest {
    private lateinit var worker: AblyConnectionStateChangeWorker
    private val connectionStateChange = ConnectionStateChange(ConnectionState.OFFLINE, null)
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = AblyConnectionStateChangeWorker(connectionStateChange, corePublisher, null)
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
    fun `should set the last connection state change`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.lastConnectionStateChange = connectionStateChange
        }
    }

    @Test
    fun `should update trackable state for all trackables`() {
        // given
        val firstTrackable = Trackable("first")
        val secondTrackable = Trackable("second")
        val thirdTrackable = Trackable("third")
        val allTrackables = mutableSetOf(firstTrackable, secondTrackable, thirdTrackable)
        every { publisherProperties.trackables } returns allTrackables

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateTrackableState(publisherProperties, firstTrackable.id)
            corePublisher.updateTrackableState(publisherProperties, secondTrackable.id)
            corePublisher.updateTrackableState(publisherProperties, thirdTrackable.id)
        }
    }
}
