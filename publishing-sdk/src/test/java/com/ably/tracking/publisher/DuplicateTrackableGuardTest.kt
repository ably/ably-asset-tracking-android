package com.ably.tracking.publisher

import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.guards.DefaultDuplicateTrackableGuard
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.updatedworkerqueue.workers.AddTrackableResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DuplicateTrackableGuardTest {
    private lateinit var duplicateTrackableGuard: DuplicateTrackableGuard

    @Before
    fun setup() {
        duplicateTrackableGuard = DefaultDuplicateTrackableGuard()
    }

    @Test
    fun `should return false if the trackable is not being added`() {
        // given
        val trackable = anyTrackable()

        // when
        val isAddingTrackable = duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should return true if the trackable is being added`() {
        // given
        val trackable = anyTrackable()

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        val isAddingTrackable = duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertTrue(isAddingTrackable)
    }

    @Test
    fun `should return false if the trackable was being added but already finished`() {
        // given
        val trackable = anyTrackable()

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())
        val isAddingTrackable = duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should return false if the trackable was being added but then it was cleared`() {
        // given
        val trackable = anyTrackable()

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.clear(trackable)
        val isAddingTrackable = duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should call duplicate handlers when adding process finishes`() {
        // given
        val trackable = anyTrackable()
        var wasDuplicateHandlerCalled = false

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.saveDuplicateAddHandler(trackable) { wasDuplicateHandlerCalled = true }
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())

        // then
        Assert.assertTrue(wasDuplicateHandlerCalled)
    }

    @Test
    fun `should call duplicate handlers with the same result from the adding process`() {
        // given
        val trackable = anyTrackable()
        val addingResult = anyResult()
        var duplicateHandlerResult: Result<AddTrackableResult>? = null

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.saveDuplicateAddHandler(trackable) { duplicateHandlerResult = it }
        duplicateTrackableGuard.finishAddingTrackable(trackable, addingResult)

        // then
        Assert.assertNotNull(duplicateHandlerResult)
        Assert.assertEquals(addingResult, duplicateHandlerResult)
    }

    @Test
    fun `should not call duplicate handlers if they were already called`() {
        // given
        val trackable = anyTrackable()
        var duplicateHandlerCallCounter = 0

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.saveDuplicateAddHandler(trackable) { duplicateHandlerCallCounter++ }
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())

        // then
        Assert.assertEquals(1, duplicateHandlerCallCounter)
    }

    @Test
    fun `should not call duplicate handlers when the trackable was cleared`() {
        // given
        val trackable = anyTrackable()
        var wasDuplicateHandlerCalled = false

        // when
        duplicateTrackableGuard.startAddingTrackable(trackable)
        duplicateTrackableGuard.saveDuplicateAddHandler(trackable) { wasDuplicateHandlerCalled = true }
        duplicateTrackableGuard.clear(trackable)
        duplicateTrackableGuard.finishAddingTrackable(trackable, anyResult())

        // then
        Assert.assertFalse(wasDuplicateHandlerCalled)
    }

    private fun anyTrackable() = Trackable("test")

    private fun anyResult() = Result.success<AddTrackableResult>(MutableStateFlow(TrackableState.Online))
}
