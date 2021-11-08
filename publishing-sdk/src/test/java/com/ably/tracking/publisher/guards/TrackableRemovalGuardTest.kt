package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
        trackableRemovalGuard.markForRemoval(trackable)

        //when
        trackableRemovalGuard.removeMarked(trackable)

        //then
        assertFalse(trackableRemovalGuard.markedForRemoval(trackable))
    }

    @Test
    fun `removing the wrong trackable does not remove the trackable from removal list`() {
        // given
        val trackable = Trackable("sample")
        val otherTrackable = Trackable("sample2")
        trackableRemovalGuard.markForRemoval(trackable)

        //when
        trackableRemovalGuard.removeMarked(otherTrackable)

        //then
        assertTrue(trackableRemovalGuard.markedForRemoval(trackable))
    }
    @Test
    fun `adding the same trackable twice and removing it once removes it from removal list`() {
        // given
        val trackable = Trackable("sample")
        val sameTrackable = Trackable("sample")
        trackableRemovalGuard.markForRemoval(trackable)
        trackableRemovalGuard.markForRemoval(trackable)

        //when
        trackableRemovalGuard.removeMarked(sameTrackable)

        //then
        assertFalse(trackableRemovalGuard.markedForRemoval(trackable))
    }
}