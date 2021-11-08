package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class TrackableRemovalGuardTest {
    val trackableRemovalGuard = TrackableRemovalGuard()

    @Test
    fun `marking trackable for removal really marks it for removal`() {
        // given
        val trackable = Trackable("sample")
        //when
        trackableRemovalGuard.markForRemoval(trackable)
        //then
        assertTrue(trackableRemovalGuard.markedForRemoval(trackable))
    }

    @Test
    fun `not marking trackable for removal not marks it for removal`() {
        // given
        val trackable = Trackable("sample")
        //when
        //nothing happens here
        //then
        assertFalse(trackableRemovalGuard.markedForRemoval(trackable))
    }

    @Test
    fun `removing marked trackable removes it from removal list`() {
        // given
        val trackable = Trackable("sample")
        //when
        trackableRemovalGuard.markForRemoval(trackable)
        trackableRemovalGuard.removeMarked(trackable)
        //then
        assertFalse(trackableRemovalGuard.markedForRemoval(trackable))
    }

    @Test
    fun `removing the wrong trackable does not remove the trackable from removal list`() {
        // given
        val trackable = Trackable("sample")
        val otherTrackable = Trackable("sample2")
        //when
        trackableRemovalGuard.markForRemoval(trackable)
        trackableRemovalGuard.removeMarked(otherTrackable)
        //then
        assertTrue(trackableRemovalGuard.markedForRemoval(trackable))
    }
}