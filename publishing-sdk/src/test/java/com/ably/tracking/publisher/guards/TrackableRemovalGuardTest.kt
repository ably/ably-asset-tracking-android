package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.lang.Exception
import com.ably.tracking.common.ResultHandler as ResultHandler1

class TrackableRemovalGuardTest {
    private val trackableRemovalGuard = TrackableRemovalGuard()

    @Test
    fun `marking trackable for removal really marks it for removal`() {
        // given
        val trackable = Trackable("sample")
        trackableRemovalGuard.markForRemoval(trackable) {

        }
        // then
        assertTrue(trackableRemovalGuard.isMarkedForRemoval(trackable))
    }

    @Test
    fun `not marking trackable for removal not marks it for removal`() {
        // given
        val trackable = Trackable("sample")
        // when
        // nothing happens here
        // then
        assertFalse(trackableRemovalGuard.isMarkedForRemoval(trackable))
    }

    @Test
    fun `removing marked trackable removes it from removal list`() {
        // given
        val trackable = Trackable("sample")
        trackableRemovalGuard.markForRemoval(trackable) {}

        // when
        trackableRemovalGuard.removeMarked(trackable, Result.success(true))

        // then
        assertFalse(trackableRemovalGuard.isMarkedForRemoval(trackable))
    }

    @Test
    fun `removing the wrong trackable does not remove the trackable from removal list`() {
        // given
        val trackable = Trackable("sample")
        val otherTrackable = Trackable("sample2")
        trackableRemovalGuard.markForRemoval(trackable) {}

        // when
        trackableRemovalGuard.removeMarked(otherTrackable, Result.success(true))

        // then
        assertTrue(trackableRemovalGuard.isMarkedForRemoval(trackable))
    }

    @Test
    fun `adding the same trackable twice and removing it once removes it from removal list`() {
        // given
        val trackable = Trackable("sample")
        val sameTrackable = Trackable("sample")
        trackableRemovalGuard.markForRemoval(trackable) {}
        trackableRemovalGuard.markForRemoval(trackable) {}

        // when
        trackableRemovalGuard.removeMarked(sameTrackable, Result.success(true))

        // then
        assertFalse(trackableRemovalGuard.isMarkedForRemoval(trackable))
    }

    @Test
    fun `handlers are called after associated trackable was removed`() {
        // given
        val trackable = Trackable("sample")
        var called1 = false
        var called2 = false
        trackableRemovalGuard.markForRemoval(trackable) {
            called1 = true
        }
        //add it one more time
        trackableRemovalGuard.markForRemoval(trackable) {
            called2 = true
        }

        // when
        trackableRemovalGuard.removeMarked(trackable, Result.success(true))

        // then
        assertTrue(called1 && called2)
    }

    @Test
    fun `handlers are not called if associated trackable was not removed`() {
        // given
        val trackable = Trackable("sample")
        var called1 = false
        var called2 = false

        trackableRemovalGuard.markForRemoval(trackable) {
            called1 = true
        }
        //add it one more time
        trackableRemovalGuard.markForRemoval(trackable) {
            called2 = true
        }

        // when nothing happens

        // then
        assertFalse(called1 || called2)
    }

    @Test
    fun `handler is called with success if removal was successful`() {
        // given
        val trackable = Trackable("sample")

        trackableRemovalGuard.markForRemoval(trackable) {
            //then
            assertTrue(it.isSuccess)
        }
        // when
        trackableRemovalGuard.removeMarked(trackable, Result.success(true))
    }

    @Test
    fun `handler is called with failure if removal was not successful`() {
        // given
        val trackable = Trackable("sample")

        trackableRemovalGuard.markForRemoval(trackable) {
            //then
            assertTrue(it.isFailure)
        }
        // when
        trackableRemovalGuard.removeMarked(trackable, Result.failure(Exception("simple")))
    }

}
