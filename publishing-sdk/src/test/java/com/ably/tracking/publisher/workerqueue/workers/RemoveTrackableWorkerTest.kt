package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.workerqueue.assertNotNullAndExecute
import com.ably.tracking.publisher.workerqueue.results.RemoveTrackableWorkResult
import com.ably.tracking.test.common.mockSuspendingDisconnect
import com.ably.tracking.test.common.mockSuspendingDisconnectSuccessAndCapturePresenceData
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RemoveTrackableWorkerTest {
    private lateinit var worker: RemoveTrackableWorker

    private val trackable = Trackable("testtrackable")
    private val resultCallbackFunction: ResultCallbackFunction<Boolean> = {}
    private val ably = mockk<Ably>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val duplicateTrackableGuard = mockk<DuplicateTrackableGuard>(relaxed = true)
    private val trackableRemovalGuard = mockk<TrackableRemovalGuard>(relaxed = true)

    @Before
    fun setup() {
        worker = RemoveTrackableWorker(trackable, resultCallbackFunction, ably)
        every { publisherProperties.duplicateTrackableGuard } returns duplicateTrackableGuard
        every { publisherProperties.trackableRemovalGuard } returns trackableRemovalGuard
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should return only async work when removing a trackable that is already added`() {
        // given
        mockTrackableAlreadyAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNotNull(result.asyncWork)
    }

    @Test
    fun `should disconnect the trackable from Ably when removing a trackable that is already added`() {
        runBlocking {
            // given
            mockTrackableAlreadyAdded()

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            coVerify(exactly = 1) {
                ably.disconnect(trackable.id, any())
            }
        }
    }

    @Test
    fun `should use a copy of presence data when disconnecting the trackable from Ably when removing a trackable that is already added`() {
        runBlocking {
            // given
            mockTrackableAlreadyAdded()
            val originalPresenceData = PresenceData("type")
            every { publisherProperties.presenceData } returns originalPresenceData
            val presenceDataSlot = ably.mockSuspendingDisconnectSuccessAndCapturePresenceData(trackable.id)

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            val usedPresenceData = presenceDataSlot.captured
            Assert.assertEquals(originalPresenceData, usedPresenceData)
            Assert.assertNotSame(originalPresenceData, usedPresenceData)
        }
    }

    @Test
    fun `should return a 'Success' result when disconnect was successful when removing a trackable that is already added`() {
        runBlocking {
            // given
            mockTrackableAlreadyAdded()
            ably.mockSuspendingDisconnect(trackable.id, Result.success(Unit))

            // when
            val asyncWorkResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncWorkResult is RemoveTrackableWorkResult.Success)
            val success = asyncWorkResult as RemoveTrackableWorkResult.Success
            Assert.assertEquals(trackable, success.trackable)
            Assert.assertEquals(resultCallbackFunction, success.callbackFunction)
        }
    }

    @Test
    fun `should return a 'Failure' result when disconnect failed when removing a trackable that is already added`() {
        runBlocking {
            // given
            mockTrackableAlreadyAdded()
            val disconnectException = Exception("test")
            ably.mockSuspendingDisconnect(trackable.id, Result.failure(disconnectException))

            // when
            val asyncWorkResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncWorkResult is RemoveTrackableWorkResult.Fail)
            val fail = asyncWorkResult as RemoveTrackableWorkResult.Fail
            Assert.assertEquals(resultCallbackFunction, fail.callbackFunction)
            Assert.assertEquals(disconnectException, fail.exception)
        }
    }

    @Test
    fun `should return empty result when removing a trackable that is currently being added`() {
        // given
        mockTrackableIsCurrentlyBeingAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should mark trackable for removal when removing a trackable that is currently being added`() {
        // given
        mockTrackableIsCurrentlyBeingAdded()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            trackableRemovalGuard.markForRemoval(trackable, resultCallbackFunction)
        }
    }

    @Test
    fun `should return only sync work result when removing a trackable that is not already added or currently being added`() {
        // given
        mockTrackableNotPresent()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNotNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should return a 'NotPresent' result when removing a trackable that is not already added or currently being added`() {
        // given
        mockTrackableNotPresent()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(result.syncWorkResult is RemoveTrackableWorkResult.NotPresent)
        val notPresent = result.syncWorkResult as RemoveTrackableWorkResult.NotPresent
        Assert.assertEquals(resultCallbackFunction, notPresent.callbackFunction)
    }

    private fun mockTrackableAlreadyAdded() {
        every { publisherProperties.trackables.contains(trackable) } returns true
    }

    private fun mockTrackableIsCurrentlyBeingAdded() {
        every { duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) } returns true
    }

    private fun mockTrackableNotPresent() {
        every { publisherProperties.trackables.contains(trackable) } returns false
        every { duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) } returns false
    }
}
