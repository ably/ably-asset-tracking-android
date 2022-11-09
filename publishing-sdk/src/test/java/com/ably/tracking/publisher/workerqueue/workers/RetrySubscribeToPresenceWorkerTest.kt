package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.assertNotNullAndExecute
import com.ably.tracking.publisher.workerqueue.results.RetrySubscribeToPresenceWorkResult
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal class RetrySubscribeToPresenceWorkerTest {
    lateinit var worker: RetrySubscribeToPresenceWorker
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val trackable = Trackable("test-trackable")
    private val ably = mockk<Ably>(relaxed = true)
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    @Before
    fun setUp() {
        worker = RetrySubscribeToPresenceWorker(trackable, ably, null, presenceUpdateListener)
        mockTrackableIsAdded()
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should return trackable removed if the trackable is not in the added trackable set`() {
        // given
        mockTrackableIsNotAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(result.syncWorkResult is RetrySubscribeToPresenceWorkResult.TrackableRemoved)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should not try to subscribe to presence if the trackable is not in the added trackable set`() {
        // given
        mockTrackableIsNotAdded()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            ably.subscribeForPresenceMessages(trackable.id, any(), any<(Result<Unit>) -> Unit>())
        }
    }

    @Test
    fun `should return channel failed if the channel went to the failed state`() {
        runBlocking {
            // given
            mockChannelStateChange(ConnectionState.FAILED)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is RetrySubscribeToPresenceWorkResult.ChannelFailed)
        }
    }

    @Test
    fun `should not try to subscribe to presence if the channel went to the failed state`() {
        runBlocking {
            // given
            mockChannelStateChange(ConnectionState.FAILED)

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            verify(exactly = 0) {
                ably.subscribeForPresenceMessages(trackable.id, any(), any<(Result<Unit>) -> Unit>())
            }
        }
    }

    @Test
    fun `should return success if the channel went to the online state and subscribe to presence was successful`() {
        runBlocking {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockSubscribeToPresenceSuccess(trackable.id)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is RetrySubscribeToPresenceWorkResult.Success)
            // verify result content
            val successResult = asyncResult as RetrySubscribeToPresenceWorkResult.Success
            Assert.assertEquals(trackable, successResult.trackable)
        }
    }

    @Test
    fun `should return failure if the channel went to the online state but subscribe to presence has failed`() {
        runBlocking {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockSubscribeToPresenceError(trackable.id)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is RetrySubscribeToPresenceWorkResult.Failure)
            // verify result content
            val failureResult = asyncResult as RetrySubscribeToPresenceWorkResult.Failure
            Assert.assertEquals(trackable, failureResult.trackable)
            Assert.assertEquals(presenceUpdateListener, failureResult.presenceUpdateListener)
        }
    }

    private fun mockChannelStateChange(newState: ConnectionState) {
        val channelStateListenerSlot = slot<(ConnectionStateChange) -> Unit>()
        every {
            ably.subscribeForChannelStateChange(trackable.id, capture(channelStateListenerSlot))
        } answers {
            channelStateListenerSlot.captured.invoke(ConnectionStateChange(newState, null))
        }
    }

    private fun mockTrackableIsAdded() {
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
    }

    private fun mockTrackableIsNotAdded() {
        every { publisherProperties.trackables } returns mutableSetOf()
    }
}
