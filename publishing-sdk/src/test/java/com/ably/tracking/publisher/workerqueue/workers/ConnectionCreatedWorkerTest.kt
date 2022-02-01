package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.workerqueue.assertNotNullAndExecute
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.test.common.mockDisconnectSuccess
import com.ably.tracking.test.common.mockDisconnectSuccessAndCapturePresenceData
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import com.ably.tracking.test.common.mockSuspendingDisconnect
import com.ably.tracking.test.common.mockSuspendingDisconnectSuccessAndCapturePresenceData
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ConnectionCreatedWorkerTest {
    private lateinit var worker: ConnectionCreatedWorker
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val trackableRemovalGuard = mockk<TrackableRemovalGuard>(relaxed = true)
    private val ably = mockk<Ably>(relaxed = true)
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    @Before
    fun setUp() {
        worker = ConnectionCreatedWorker(trackable, resultCallbackFunction, ably, presenceUpdateListener)
        every { publisherProperties.trackableRemovalGuard } returns trackableRemovalGuard
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should return only async result when executing normally`() {
        // given

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNotNull(result.asyncWork)
    }

    @Test
    fun `should return presence success result when executing normally and presence enter was successful`() {
        runBlocking {
            // given
            ably.mockSubscribeToPresenceSuccess(trackable.id)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is ConnectionCreatedWorkResult.PresenceSuccess)
            // verify result content
            val presenceSuccessResult = asyncResult as ConnectionCreatedWorkResult.PresenceSuccess
            Assert.assertEquals(trackable, presenceSuccessResult.trackable)
            Assert.assertEquals(resultCallbackFunction, presenceSuccessResult.callbackFunction)
            Assert.assertEquals(presenceUpdateListener, presenceSuccessResult.presenceUpdateListener)
        }
    }

    @Test
    fun `should return presence failure result when executing normally and presence enter failed`() {
        runBlocking {
            // given
            ably.mockSubscribeToPresenceError(trackable.id)
            ably.mockDisconnectSuccess(trackable.id)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is ConnectionCreatedWorkResult.PresenceFail)
            // verify result content
            val presenceFailResult = asyncResult as ConnectionCreatedWorkResult.PresenceFail
            Assert.assertEquals(trackable, presenceFailResult.trackable)
            Assert.assertEquals(resultCallbackFunction, presenceFailResult.callbackFunction)
            Assert.assertNotNull(presenceFailResult.exception)
        }
    }

    @Test
    fun `should disconnect from Ably when executing normally and presence enter failed`() {
        runBlocking {
            // given
            ably.mockSubscribeToPresenceError(trackable.id)
            ably.mockDisconnectSuccess(trackable.id)

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            coVerify(exactly = 1) {
                ably.disconnect(trackable.id, any(), any())
            }
        }
    }

    @Test
    fun `should use a copy of presence data when disconnecting when executing normally and presence enter failed`() {
        runBlocking {
            // given
            val originalPresenceData = PresenceData("test-type")
            mockPresenceData(originalPresenceData)
            val presenceDataSlot = ably.mockDisconnectSuccessAndCapturePresenceData(trackable.id)
            ably.mockSubscribeToPresenceError(trackable.id)

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            val disconnectPresenceData = presenceDataSlot.captured
            Assert.assertNotSame("A copy of presence data should be used", originalPresenceData, disconnectPresenceData)
            Assert.assertEquals("Presence data should be an exact copy", originalPresenceData, disconnectPresenceData)
        }
    }

    @Test
    fun `should return only async result when trackable removal was requested`() {
        // given
        mockTrackableRemovalRequested()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNotNull(result.asyncWork)
    }

    @Test
    fun `should return removal request result when trackable removal was requested`() {
        runBlocking {
            // given
            mockTrackableRemovalRequested()
            val disconnectResult = Result.success(Unit)
            ably.mockSuspendingDisconnect(trackable.id, disconnectResult)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is ConnectionCreatedWorkResult.RemovalRequested)
            // verify result content
            val removalRequestedResult = asyncResult as ConnectionCreatedWorkResult.RemovalRequested
            Assert.assertEquals(trackable, removalRequestedResult.trackable)
            Assert.assertEquals(resultCallbackFunction, removalRequestedResult.callbackFunction)
            Assert.assertEquals(disconnectResult, removalRequestedResult.result)
        }
    }

    @Test
    fun `should disconnect from Ably when trackable removal was requested`() {
        runBlocking {
            // given
            mockTrackableRemovalRequested()

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            coVerify(exactly = 1) {
                ably.disconnect(trackable.id, any())
            }
        }
    }

    @Test
    fun `should use a copy of presence data when disconnecting when trackable removal was requested`() {
        runBlocking {
            // given
            val originalPresenceData = PresenceData("test-type")
            mockPresenceData(originalPresenceData)
            val presenceDataSlot = ably.mockSuspendingDisconnectSuccessAndCapturePresenceData(trackable.id)
            mockTrackableRemovalRequested()

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            val disconnectPresenceData = presenceDataSlot.captured
            Assert.assertNotSame("A copy of presence data should be used", originalPresenceData, disconnectPresenceData)
            Assert.assertEquals("Presence data should be an exact copy", originalPresenceData, disconnectPresenceData)
        }
    }

    private fun mockTrackableRemovalRequested() {
        every { trackableRemovalGuard.isMarkedForRemoval(trackable) } returns true
    }

    private fun mockPresenceData(presenceData: PresenceData) {
        every { publisherProperties.presenceData } returns presenceData
    }
}
