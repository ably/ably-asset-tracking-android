package com.ably.tracking.publisher

import com.ably.tracking.TrackableState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TrackableAddingStateTest {
    private lateinit var trackableAddingState: TrackableAddingState

    @Before
    fun setup() {
        trackableAddingState = TrackableAddingState()
    }

    @Test
    fun `should return false if the trackable is not being added`() {
        // given
        val trackable = anyTrackable()

        // when
        val isAddingTrackable = trackableAddingState.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should return true if the trackable is being added`() {
        // given
        val trackable = anyTrackable()

        // when
        trackableAddingState.startAddingTrackable(trackable)
        val isAddingTrackable = trackableAddingState.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertTrue(isAddingTrackable)
    }

    @Test
    fun `should return false if the trackable was being added but already finished`() {
        // given
        val trackable = anyTrackable()

        // when
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.finishAddingTrackable(trackable, anyResult())
        val isAddingTrackable = trackableAddingState.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should return false if the trackable was being added but then it was cleared`() {
        // given
        val trackable = anyTrackable()

        // when
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.clear(trackable)
        val isAddingTrackable = trackableAddingState.isCurrentlyAddingTrackable(trackable)

        // then
        Assert.assertFalse(isAddingTrackable)
    }

    @Test
    fun `should call duplicate handlers when adding process finishes`() {
        // given
        val trackable = anyTrackable()
        var wasDuplicateHandlerCalled = false

        // when
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.saveDuplicateAddHandler(trackable) { wasDuplicateHandlerCalled = true }
        trackableAddingState.finishAddingTrackable(trackable, anyResult())

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
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.saveDuplicateAddHandler(trackable) { duplicateHandlerResult = it }
        trackableAddingState.finishAddingTrackable(trackable, addingResult)

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
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.saveDuplicateAddHandler(trackable) { duplicateHandlerCallCounter++ }
        trackableAddingState.finishAddingTrackable(trackable, anyResult())
        trackableAddingState.finishAddingTrackable(trackable, anyResult())
        trackableAddingState.finishAddingTrackable(trackable, anyResult())

        // then
        Assert.assertEquals(1, duplicateHandlerCallCounter)
    }

    @Test
    fun `should not call duplicate handlers when the trackable was cleared`() {
        // given
        val trackable = anyTrackable()
        var wasDuplicateHandlerCalled = false

        // when
        trackableAddingState.startAddingTrackable(trackable)
        trackableAddingState.saveDuplicateAddHandler(trackable) { wasDuplicateHandlerCalled = true }
        trackableAddingState.clear(trackable)
        trackableAddingState.finishAddingTrackable(trackable, anyResult())

        // then
        Assert.assertFalse(wasDuplicateHandlerCalled)
    }

    private fun anyTrackable() = Trackable("test")

    private fun anyResult() = Result.success<AddTrackableResult>(MutableStateFlow(TrackableState.Online))
}
