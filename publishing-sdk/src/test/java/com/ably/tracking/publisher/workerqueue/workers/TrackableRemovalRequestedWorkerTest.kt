package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.updatedworkerqueue.workers.AddTrackableCallbackFunction
import com.ably.tracking.publisher.updatedworkerqueue.workers.AddTrackableResult
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TrackableRemovalRequestedWorkerTest {
    private lateinit var worker: TrackableRemovalRequestedWorker
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val trackableRemovalGuard = TrackableRemovalGuardSpy()
    private val duplicateTrackableGuard = DuplicateTrackableGuardSpy()
    private val ably = mockk<Ably>(relaxed = true)

    @Before
    fun setUp() {
        prepareWorkerWithResult(Result.success(Unit))
        every { publisherProperties.duplicateTrackableGuard } returns duplicateTrackableGuard
        every { publisherProperties.trackableRemovalGuard } returns trackableRemovalGuard
    }

    @After
    fun cleanUp() {
        clearAllMocks()
        duplicateTrackableGuard.reset()
        trackableRemovalGuard.reset()
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
    fun `should always call the add trackable callback with a trackable removal requested exception`() {
        // given
        val addTrackableResultSlot = captureResultCallbackFunctionResult()

        // when
        worker.doWork(publisherProperties)

        // then
        val addTrackableResult = addTrackableResultSlot.captured
        Assert.assertTrue(addTrackableResult.isFailure)
        Assert.assertTrue(addTrackableResult.exceptionOrNull() is RemoveTrackableRequestedException)
    }

    @Test
    fun `should always finish adding the trackable with a trackable removal requested exception`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        val finishAddingTrackableResult = duplicateTrackableGuard.lastFinishAddingTrackableResult!!
        Assert.assertTrue(finishAddingTrackableResult.isFailure)
        Assert.assertTrue(finishAddingTrackableResult.exceptionOrNull() is RemoveTrackableRequestedException)
    }

    @Test
    fun `should mark removal success if result is successful`() {
        // given
        prepareWorkerWithResult(Result.success(Unit))

        // when
        worker.doWork(publisherProperties)

        // then
        val removeMarkedResult = trackableRemovalGuard.lastRemoveMarkedResult!!
        Assert.assertTrue(removeMarkedResult.isSuccess)
        Assert.assertTrue(removeMarkedResult.getOrNull()!!)
    }

    @Test
    fun `should mark removal failure if result is failure`() {
        // given
        prepareWorkerWithResult(Result.failure(Exception()))

        // when
        worker.doWork(publisherProperties)

        // then
        val removeMarkedResult = trackableRemovalGuard.lastRemoveMarkedResult!!
        Assert.assertTrue(removeMarkedResult.isFailure)
        Assert.assertNotNull(removeMarkedResult.exceptionOrNull())
    }

    private fun prepareWorkerWithResult(result: Result<Unit>) {
        worker = TrackableRemovalRequestedWorker(trackable, resultCallbackFunction, ably, result)
    }

    private fun captureResultCallbackFunctionResult(): CapturingSlot<Result<StateFlow<TrackableState>>> {
        val resultCallbackFunctionResult = slot<Result<StateFlow<TrackableState>>>()
        every { resultCallbackFunction.invoke(capture(resultCallbackFunctionResult)) } just runs
        return resultCallbackFunctionResult
    }
}

/**
 * This class is a test utility that we had to create because the Mockk library couldn't mock the
 * [finishAddingTrackable] method (i.e. the result parameter). We've previously noticed that this
 * library has troubles with the Kotlin's [Result] type and couldn't find any workaround, hence
 * this class was created. This is the same issue that's behind [TrackableRemovalGuardSpy].
 */
private class DuplicateTrackableGuardSpy : DuplicateTrackableGuard {
    var lastFinishAddingTrackableResult: Result<AddTrackableResult>? = null

    override fun startAddingTrackable(trackable: Trackable) = Unit

    override fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>) {
        lastFinishAddingTrackableResult = result
    }

    override fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean = false

    override fun isCurrentlyAddingAnyTrackable(): Boolean = false

    override fun saveDuplicateAddHandler(trackable: Trackable, callbackFunction: AddTrackableCallbackFunction) = Unit

    override fun clear(trackable: Trackable) = Unit

    override fun clearAll() = Unit

    fun reset() {
        lastFinishAddingTrackableResult = null
    }
}

/**
 * This class is a test utility that we had to create because the Mockk library couldn't mock the
 * [removeMarked] method (i.e. the result parameter). We've previously noticed that this
 * library has troubles with the Kotlin's [Result] type and couldn't find any workaround, hence
 * this class was created. This is the same issue that's behind [DuplicateTrackableGuardSpy].
 */
private class TrackableRemovalGuardSpy : TrackableRemovalGuard {
    var lastRemoveMarkedResult: Result<Boolean>? = null

    override fun markForRemoval(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>) = Unit

    override fun isMarkedForRemoval(trackable: Trackable): Boolean = false

    override fun removeMarked(trackable: Trackable, result: Result<Boolean>) {
        lastRemoveMarkedResult = result
    }

    override fun clearAll() = Unit

    fun reset() {
        lastRemoveMarkedResult = null
    }
}
