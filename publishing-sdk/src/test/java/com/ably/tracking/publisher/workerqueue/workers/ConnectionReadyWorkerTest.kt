package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.workerqueue.assertNotNullAndExecute
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.test.common.mockSuspendingDisconnect
import com.ably.tracking.test.common.mockSuspendingDisconnectSuccessAndCapturePresenceData
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ConnectionReadyWorkerTest {
    private lateinit var worker: ConnectionReadyWorker
    private val trackable = Trackable("test-trackable")
    private val trackables = mockk<MutableSet<Trackable>>(relaxed = true)
    private val trackableStates = mockk<MutableMap<String, TrackableState>>(relaxed = true)
    private val trackableStateFlows = mockk<MutableMap<String, MutableStateFlow<TrackableState>>>(relaxed = true)
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val trackableRemovalGuard = mockk<TrackableRemovalGuard>(relaxed = true)
    private val duplicateTrackableGuard = mockk<DuplicateTrackableGuard>(relaxed = true)
    private val ably = mockk<Ably>(relaxed = true)
    private val hooks = mockk<DefaultCorePublisher.Hooks>(relaxed = true)
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val connectionStateChangeListener: (ConnectionStateChange) -> Unit = {}

    @Before
    fun setUp() {
        worker = ConnectionReadyWorker(
            trackable,
            resultCallbackFunction,
            ably,
            hooks,
            corePublisher,
            connectionStateChangeListener
        )
        every { publisherProperties.trackableRemovalGuard } returns trackableRemovalGuard
        every { publisherProperties.duplicateTrackableGuard } returns duplicateTrackableGuard
        every { publisherProperties.trackables } returns trackables
        every { publisherProperties.trackableStateFlows } returns trackableStateFlows
        every { trackableStateFlows[trackable.id] } returns null
        every { publisherProperties.trackableStates } returns trackableStates
        every { trackableStates[trackable.id] } returns null
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `executing normally`() {
        // WHEN
        val result = worker.doWork(publisherProperties)

        // THEN

        // should return empty result
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)

        verify(exactly = 1) {
            // should subscribe to Ably channel state updates
            ably.subscribeForChannelStateChange(trackable.id, any())

            // should add the trackable to the tracked trackables
            trackables.add(trackable)

            // should update the tracked trackables
            corePublisher.updateTrackables(any())

            // should calculate a resolution for the added trackable
            corePublisher.resolveResolution(trackable, any())

            // should set a state flow for the trackable
            trackableStateFlows[trackable.id] = any()

            // should update state flows
            corePublisher.updateTrackableStateFlows(any())

            // should set the initial trackable state to offline
            trackableStates[trackable.id] = TrackableState.Offline()

            // should finish adding the trackable with success
            duplicateTrackableGuard.finishAddingTrackable(trackable, Result.success(any()))
        }
    }

    @Test
    fun `should start location updates if is not already tracking when executing normally`() {
        // given
        every { publisherProperties.isTracking } returns false

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.startLocationUpdates(any())
        }
    }

    @Test
    fun `should not start location updates if is already tracking when executing normally`() {
        // given
        every { publisherProperties.isTracking } returns true

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.startLocationUpdates(any())
        }
    }

    @Test
    fun `should call the adding trackable callback with a success when executing normally`() {
        // given
        val callbackResultSlot = slot<Result<StateFlow<TrackableState>>>()
        every { resultCallbackFunction.invoke(capture(callbackResultSlot)) } just runs

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertTrue(callbackResultSlot.captured.isSuccess)
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
    fun `should return trackable removal work result when trackable removal was requested`() {
        runBlocking {
            // given
            mockTrackableRemovalRequested()
            val disconnectResult = Result.success(Unit)
            ably.mockSuspendingDisconnect(trackable.id, disconnectResult)

            // when
            val asyncResult = worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            Assert.assertTrue(asyncResult is ConnectionReadyWorkResult.RemovalRequested)
            // verify result content
            val removalRequestedResult = asyncResult as ConnectionReadyWorkResult.RemovalRequested
            Assert.assertEquals(trackable, removalRequestedResult.trackable)
            Assert.assertEquals(resultCallbackFunction, removalRequestedResult.callbackFunction)
            Assert.assertEquals(disconnectResult, removalRequestedResult.result)
        }
    }

    @Test
    fun `should use a copy of presence data when disconnecting from Ably when trackable removal was requested`() {
        runBlocking {
            // given
            mockTrackableRemovalRequested()
            val originalPresenceData = PresenceData("test")
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
    fun `should not perform any of the normal operations when trackable removal was requested`() {
        runBlocking {
            // given
            mockTrackableRemovalRequested()

            // when
            worker.doWork(publisherProperties).asyncWork.assertNotNullAndExecute()

            // then
            verify(exactly = 0) {
                ably.subscribeForChannelStateChange(trackable.id, any())
                corePublisher.startLocationUpdates(any())
                trackables.add(trackable)
                corePublisher.updateTrackables(any())
                corePublisher.resolveResolution(trackable, any())
                trackableStateFlows[trackable.id] = any()
                corePublisher.updateTrackableStateFlows(any())
                trackableStates[trackable.id] = any()
                resultCallbackFunction.invoke(any())
                duplicateTrackableGuard.finishAddingTrackable(trackable, Result.success(any()))
            }
        }
    }

    private fun mockTrackableRemovalRequested() {
        every { trackableRemovalGuard.isMarkedForRemoval(trackable) } returns true
    }
}
