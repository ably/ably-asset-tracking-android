package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.test.common.anyLocation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DisconnectSuccessWorkerTest {
    private lateinit var worker: DisconnectSuccessWorker

    private val trackable = Trackable("testtrackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<Unit>>(relaxed = true)
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val recalculateResolutionCallbackFunction = mockk<() -> Unit>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = DisconnectSuccessWorker(
            trackable,
            resultCallbackFunction,
            corePublisher,
            recalculateResolutionCallbackFunction,
        )
    }

    @After
    fun cleanUp() {
        clearAllMocks()
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
    fun `should remove the trackable from the tracked trackables`() {
        // given
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
        Assert.assertTrue(publisherProperties.trackables.contains(trackable))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.trackables.contains(trackable))
    }

    @Test
    fun `should remove the trackable state flow`() {
        // given
        every { publisherProperties.trackableStateFlows } returns mutableMapOf(
            trackable.id to MutableStateFlow(TrackableState.Offline())
        )
        Assert.assertTrue(publisherProperties.trackableStateFlows.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.trackableStateFlows.contains(trackable.id))
    }

    @Test
    fun `should remove the trackable state`() {
        // given
        every { publisherProperties.trackableStates } returns mutableMapOf(
            trackable.id to TrackableState.Offline()
        )
        Assert.assertTrue(publisherProperties.trackableStates.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.trackableStates.contains(trackable.id))
    }

    @Test
    fun `should remove the trackable resolution`() {
        // given
        every { publisherProperties.resolutions } returns mutableMapOf(
            trackable.id to Resolution(Accuracy.BALANCED, 1L, 1.0)
        )
        Assert.assertTrue(publisherProperties.resolutions.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.resolutions.contains(trackable.id))
    }

    @Test
    fun `should call the location engine resolution recalculation callback if this trackable had a resolution`() {
        // given
        every { publisherProperties.resolutions } returns mutableMapOf(
            trackable.id to Resolution(Accuracy.BALANCED, 1L, 1.0)
        )
        Assert.assertTrue(publisherProperties.resolutions.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            recalculateResolutionCallbackFunction.invoke()
        }
    }

    @Test
    fun `should not call the location engine resolution recalculation callback if this trackable didn't have a resolution`() {
        // given
        every { publisherProperties.resolutions } returns mutableMapOf()
        Assert.assertFalse(publisherProperties.resolutions.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            recalculateResolutionCallbackFunction.invoke()
        }
    }

    @Test
    fun `should remove the trackable resolution requests`() {
        // given
        every { publisherProperties.requests } returns mutableMapOf(
            trackable.id to mutableMapOf()
        )
        Assert.assertTrue(publisherProperties.requests.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.requests.contains(trackable.id))
    }

    @Test
    fun `should remove the trackable last sent enhanced location`() {
        // given
        every { publisherProperties.lastSentEnhancedLocations } returns mutableMapOf(
            trackable.id to anyLocation()
        )
        Assert.assertTrue(publisherProperties.lastSentEnhancedLocations.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.lastSentEnhancedLocations.contains(trackable.id))
    }

    @Test
    fun `should remove the trackable last sent raw location`() {
        // given
        every { publisherProperties.lastSentRawLocations } returns mutableMapOf(
            trackable.id to anyLocation()
        )
        Assert.assertTrue(publisherProperties.lastSentRawLocations.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.lastSentRawLocations.contains(trackable.id))
    }

    @Test
    fun `should remove the trackable last channel connection state change`() {
        // given
        every { publisherProperties.lastChannelConnectionStateChanges } returns mutableMapOf(
            trackable.id to ConnectionStateChange(ConnectionState.OFFLINE, null)
        )
        Assert.assertTrue(publisherProperties.lastChannelConnectionStateChanges.contains(trackable.id))

        // when
        worker.doWork(publisherProperties)

        // then
        Assert.assertFalse(publisherProperties.lastChannelConnectionStateChanges.contains(trackable.id))
    }

    @Test
    fun `should update the trackables in the publisher`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateTrackables(publisherProperties)
        }
    }

    @Test
    fun `should update the trackable state flows in the publisher`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateTrackableStateFlows(publisherProperties)
        }
    }

    @Test
    fun `should notify the resolution policy that a trackable was removed`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.notifyResolutionPolicyThatTrackableWasRemoved(trackable)
        }
    }

    @Test
    fun `should remove all saved subscribers for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.removeAllSubscribers(trackable, publisherProperties)
        }
    }

    @Test
    fun `should clear all skipped enhanced locations for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.skippedEnhancedLocations.clear(trackable.id)
        }
    }

    @Test
    fun `should clear all skipped raw locations for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.skippedRawLocations.clear(trackable.id)
        }
    }

    @Test
    fun `should clear the enhanced locations publishing state for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.enhancedLocationsPublishingState.clear(trackable.id)
        }
    }

    @Test
    fun `should clear the raw locations publishing state for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.rawLocationsPublishingState.clear(trackable.id)
        }
    }

    @Test
    fun `should clear the duplicate trackable guard for the removed trackable`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.duplicateTrackableGuard.clear(trackable)
        }
    }

    @Test
    fun `should always call the result callback with a success`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resultCallbackFunction(Result.success(Unit))
        }
    }

    @Test
    fun `should clear the active trackable if the removed trackable was the active one`() {
        // given
        every { publisherProperties.active } returns trackable

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.active = null
        }
    }

    @Test
    fun `should remove the current destination if the removed trackable was the active one`() {
        // given
        every { publisherProperties.active } returns trackable

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.removeCurrentDestination(publisherProperties)
        }
    }

    @Test
    fun `should notify the Resolution Policy that there is no active trackable the removed trackable was the active one`() {
        // given
        every { publisherProperties.active } returns trackable

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
        }
    }

    @Test
    fun `should not clear the active trackable if the removed trackable was not the active one`() {
        // given
        every { publisherProperties.active } returns anyOtherTrackable()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            publisherProperties.active = null
        }
    }

    @Test
    fun `should not remove the current destination if the removed trackable was not the active one`() {
        // given
        every { publisherProperties.active } returns anyOtherTrackable()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.removeCurrentDestination(publisherProperties)
        }
    }

    @Test
    fun `should not notify the Resolution Policy that there is no active trackable the removed trackable was not the active one`() {
        // given
        every { publisherProperties.active } returns anyOtherTrackable()

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
        }
    }

    @Test
    fun `should stop location updates if the removed trackable was the last one and the publisher is currently tracking`() {
        // given
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
        every { publisherProperties.isTracking } returns true

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.stopLocationUpdates(publisherProperties)
        }
    }

    @Test
    fun `should not stop location updates if the removed trackable was not the last one`() {
        // given
        every { publisherProperties.trackables } returns mutableSetOf(trackable, anyOtherTrackable())
        every { publisherProperties.isTracking } returns true

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.stopLocationUpdates(publisherProperties)
        }
    }

    @Test
    fun `should not stop location updates if the publisher is not currently tracking`() {
        // given
        every { publisherProperties.trackables } returns mutableSetOf(trackable)
        every { publisherProperties.isTracking } returns false

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.stopLocationUpdates(publisherProperties)
        }
    }

    private fun anyOtherTrackable() = Trackable("some-other-trackable")
}
