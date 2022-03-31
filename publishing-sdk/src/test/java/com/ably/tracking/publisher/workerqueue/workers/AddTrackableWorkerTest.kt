package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.workerqueue.assertNotNullAndExecute
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.test.common.mockSuspendingConnectFailure
import com.ably.tracking.test.common.mockSuspendingConnectSuccess
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AddTrackableWorkerTest {
    private lateinit var worker: AddTrackableWorker

    // dependencies
    private val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>> = {}
    private val ably = mockk<Ably>(relaxed = true)
    private val trackable = Trackable("testtrackable")
    private val duplicateTrackableGuard = mockk<DuplicateTrackableGuard>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = AddTrackableWorker(trackable, resultCallbackFunction, {}, {}, ably)
        every { publisherProperties.duplicateTrackableGuard } returns duplicateTrackableGuard
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should return only async work when adding a trackable that is not added and not being added`() {
        // given
        mockTrackableIsNeitherAddedNorCurrentlyBeingAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNotNull(result.asyncWork)
    }

    @Test
    fun `should start adding a trackable when adding a trackable that is not added and not being added`() {
        // given
        mockTrackableIsNeitherAddedNorCurrentlyBeingAdded()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            duplicateTrackableGuard.startAddingTrackable(trackable)
        }
    }

    @Test
    fun `should return empty result when adding a trackable that is being added`() {
        // given
        mockTrackableIsCurrentlyBeingAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.asyncWork)
        Assert.assertNull(result.syncWorkResult)
    }

    @Test
    fun `should save the trackable callback function when adding a trackable that is being added`() {
        // given
        mockTrackableIsCurrentlyBeingAdded()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            duplicateTrackableGuard.saveDuplicateAddHandler(trackable, resultCallbackFunction)
        }
    }

    @Test
    fun `should return an 'AlreadyIn' result when adding a trackable that is already added`() {
        // given
        mockTrackableIsAlreadyAdded()

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.asyncWork)
        Assert.assertTrue(result.syncWorkResult is AddTrackableWorkResult.AlreadyIn)

        // also make sure it has the right content
        val alreadyIn = result.syncWorkResult as AddTrackableWorkResult.AlreadyIn
        Assert.assertEquals(resultCallbackFunction, alreadyIn.callbackFunction)
        Assert.assertEquals(publisherProperties.trackableStateFlows[trackable.id], alreadyIn.trackableStateFlow)
    }

    // async work tests
    @Test
    fun `should successfully add a trackable when connection was successful`() {
        runBlocking {
            // given
            mockTrackableIsNeitherAddedNorCurrentlyBeingAdded()
            ably.mockSuspendingConnectSuccess(trackable.id)

            // when
            val result = worker.doWork(publisherProperties)

            // then
            val asyncWorkResult = result.asyncWork.assertNotNullAndExecute()
            Assert.assertTrue(asyncWorkResult is AddTrackableWorkResult.Success)
            // also check content
            val success = asyncWorkResult as AddTrackableWorkResult.Success
            Assert.assertEquals(trackable, success.trackable)
            Assert.assertEquals(resultCallbackFunction, success.callbackFunction)
        }
    }

    @Test
    fun `should fail to add a trackable when connection failed`() {
        runBlocking {
            // given
            mockTrackableIsNeitherAddedNorCurrentlyBeingAdded()
            ably.mockSuspendingConnectFailure(trackable.id)

            // when
            val result = worker.doWork(publisherProperties)

            // then
            val asyncWorkResult = result.asyncWork.assertNotNullAndExecute()
            Assert.assertTrue(asyncWorkResult is AddTrackableWorkResult.Fail)
            // also check content
            val fail = asyncWorkResult as AddTrackableWorkResult.Fail
            Assert.assertEquals(trackable, fail.trackable)
            Assert.assertEquals(resultCallbackFunction, fail.callbackFunction)
        }
    }

    private fun mockTrackableIsNeitherAddedNorCurrentlyBeingAdded() {
        every { duplicateTrackableGuard.isCurrentlyAddingTrackable(any()) } returns false
    }

    private fun mockTrackableIsCurrentlyBeingAdded() {
        every { duplicateTrackableGuard.isCurrentlyAddingTrackable(any()) } returns true
    }

    private fun mockTrackableIsAlreadyAdded() {
        every { duplicateTrackableGuard.isCurrentlyAddingTrackable(any()) } returns false
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
        every { publisherProperties.trackableStateFlows } returns mutableMapOf(
            trackable.id to MutableStateFlow(TrackableState.Online)
        )
    }
}
