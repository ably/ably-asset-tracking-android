package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.test.common.createLocation
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class LocationsPublishingStateTest {
    private val trackableId = "test-trackable-id"
    private lateinit var locationsPublishingState: LocationsPublishingState<EnhancedLocationUpdate>

    @Before
    fun beforeEach() {
        locationsPublishingState = LocationsPublishingState()
    }

    @Test
    fun `Should return false if trackable has not marked any messages`() {
        // given

        // when
        val hasPendingMessage = locationsPublishingState.hasPendingMessage(trackableId)

        // then
        Assert.assertFalse(hasPendingMessage)
    }

    @Test
    fun `Should return true if trackable has marked a messages`() {
        // given
        locationsPublishingState.markMessageAsPending(trackableId)

        // when
        val hasPendingMessage = locationsPublishingState.hasPendingMessage(trackableId)

        // then
        Assert.assertTrue(hasPendingMessage)
    }

    @Test
    fun `Should return false if trackable has marked and unmarked a messages`() {
        // given
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.unmarkMessageAsPending(trackableId)

        // when
        val hasPendingMessage = locationsPublishingState.hasPendingMessage(trackableId)

        // then
        Assert.assertFalse(hasPendingMessage)
    }

    @Test
    fun `Should return false if trackable has marked multiple times and unmarked a message only once`() {
        // given
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.unmarkMessageAsPending(trackableId)

        // when
        val hasPendingMessage = locationsPublishingState.hasPendingMessage(trackableId)

        // then
        Assert.assertFalse(hasPendingMessage)
    }

    @Test
    fun `Should return true if has not retried publishing a message yet`() {
        // given

        // when
        val shouldRetryPublishing = locationsPublishingState.shouldRetryPublishing(trackableId)

        // then
        Assert.assertTrue(shouldRetryPublishing)
    }

    @Test
    fun `Should return false if has retried publishing a message once`() {
        // given
        locationsPublishingState.incrementRetryCount(trackableId)

        // when
        val shouldRetryPublishing = locationsPublishingState.shouldRetryPublishing(trackableId)

        // then
        Assert.assertFalse(shouldRetryPublishing)
    }

    @Test
    fun `Should return true if has retried publishing a message once and that message was then unmarked as pending`() {
        // given
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.incrementRetryCount(trackableId)
        locationsPublishingState.unmarkMessageAsPending(trackableId)

        // when
        val shouldRetryPublishing = locationsPublishingState.shouldRetryPublishing(trackableId)

        // then
        Assert.assertTrue(shouldRetryPublishing)
    }

    @Test
    fun `Should return null if no events were added to the waiting list`() {
        // given

        // when
        val nextWaitingEvent = locationsPublishingState.getNextWaiting(trackableId)

        // then
        Assert.assertNull(nextWaitingEvent)
    }

    @Test
    fun `Should return events from the waiting list in the order they appeared in it (FIFO)`() {
        // given
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(1))
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(2))
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(3))

        // when
        val firstNextWaitingEvent = locationsPublishingState.getNextWaiting(trackableId)
        val secondNextWaitingEvent = locationsPublishingState.getNextWaiting(trackableId)
        val thirdNextWaitingEvent = locationsPublishingState.getNextWaiting(trackableId)

        // then
        Assert.assertEquals(1, firstNextWaitingEvent!!.location.time)
        Assert.assertEquals(2, secondNextWaitingEvent!!.location.time)
        Assert.assertEquals(3, thirdNextWaitingEvent!!.location.time)
    }

    @Test
    fun `Should return null if has no more waiting events`() {
        // given
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(1))
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(2))

        // when
        locationsPublishingState.getNextWaiting(trackableId)
        locationsPublishingState.getNextWaiting(trackableId)
        val nextWaitingEvent = locationsPublishingState.getNextWaiting(trackableId)

        // then
        Assert.assertNull(nextWaitingEvent)
    }

    @Test
    fun `Should clear the state for the specified trackable`() {
        // given
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(1))
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.incrementRetryCount(trackableId)

        // when
        locationsPublishingState.clear(trackableId)

        // then
        Assert.assertNull(locationsPublishingState.getNextWaiting(trackableId))
        Assert.assertFalse(locationsPublishingState.hasPendingMessage(trackableId))
        Assert.assertTrue(locationsPublishingState.shouldRetryPublishing(trackableId))
    }

    @Test
    fun `Should only clear the state for the specified trackable`() {
        // given
        val anotherTrackableId = "another-test-trackable-id"
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(1))
        locationsPublishingState.addToWaiting(anotherTrackableId, createLocationUpdate(2))
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.markMessageAsPending(anotherTrackableId)
        locationsPublishingState.incrementRetryCount(trackableId)
        locationsPublishingState.incrementRetryCount(anotherTrackableId)

        // when
        locationsPublishingState.clear(trackableId)

        // then
        Assert.assertNotNull(locationsPublishingState.getNextWaiting(anotherTrackableId))
        Assert.assertTrue(locationsPublishingState.hasPendingMessage(anotherTrackableId))
        Assert.assertFalse(locationsPublishingState.shouldRetryPublishing(anotherTrackableId))
    }

    @Test
    fun `Should clear the state for all trackables`() {
        // given
        val anotherTrackableId = "another-test-trackable-id"
        locationsPublishingState.addToWaiting(trackableId, createLocationUpdate(1))
        locationsPublishingState.addToWaiting(anotherTrackableId, createLocationUpdate(2))
        locationsPublishingState.markMessageAsPending(trackableId)
        locationsPublishingState.markMessageAsPending(anotherTrackableId)
        locationsPublishingState.incrementRetryCount(trackableId)
        locationsPublishingState.incrementRetryCount(anotherTrackableId)

        // when
        locationsPublishingState.clearAll()

        // then
        Assert.assertNull(locationsPublishingState.getNextWaiting(trackableId))
        Assert.assertFalse(locationsPublishingState.hasPendingMessage(trackableId))
        Assert.assertTrue(locationsPublishingState.shouldRetryPublishing(trackableId))
        Assert.assertNull(locationsPublishingState.getNextWaiting(anotherTrackableId))
        Assert.assertFalse(locationsPublishingState.hasPendingMessage(anotherTrackableId))
        Assert.assertTrue(locationsPublishingState.shouldRetryPublishing(anotherTrackableId))
    }

    private fun createLocationUpdate(timestamp: Long) =
        EnhancedLocationUpdate(
            createLocation(timestamp = timestamp),
            emptyList(),
            emptyList(),
            LocationUpdateType.ACTUAL
        )
}
