package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.anyLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class DisconnectSuccessWorkerTest {

    private val trackable = Trackable("testtrackable")
    private val otherTrackable = Trackable("other-trackable")
    private val resultCallbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val recalculateResolutionCallbackFunction = mockk<() -> Unit>(relaxed = true)
    private val publisherInteractor = mockk<PublisherInteractor> {
        every { updateTrackables(any()) } just runs
        every { updateTrackableStateFlows(any()) } just runs
        every { notifyResolutionPolicyThatTrackableWasRemoved(any()) } just runs
        every { removeAllSubscribers(any(), any()) } just runs
        every { stopLocationUpdates(any()) } just runs
        every { removeCurrentDestination(any()) } just runs
        every { notifyResolutionPolicyThatActiveTrackableHasChanged(any()) } just runs
    }
    private val ably: Ably = mockk {
        coEvery { stopConnection() } returns Result.success(Unit)
    }

    private val worker = DisconnectSuccessWorker(
        trackable,
        resultCallbackFunction,
        publisherInteractor,
        recalculateResolutionCallbackFunction,
        ably
    )

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should remove the trackable from the tracked trackables`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackables).doesNotContain(trackable)
    }

    @Test
    fun `should remove the trackable state flow`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackableStateFlows[trackable.id] = MutableStateFlow(TrackableState.Offline())

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.trackableStateFlows).isEmpty()
    }

    @Test
    fun `should remove the trackable state`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackableStates[trackable.id] = TrackableState.Offline()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.trackableStates).isEmpty()
    }

    @Test
    fun `should remove the trackable resolution`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.resolutions[trackable.id] = Resolution(Accuracy.BALANCED, 1L, 1.0)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.resolutions).isEmpty()
    }

    @Test
    fun `should call the location engine resolution recalculation callback if this trackable had a resolution`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.resolutions[trackable.id] = Resolution(Accuracy.BALANCED, 1L, 1.0)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            recalculateResolutionCallbackFunction.invoke()
        }
    }

    @Test
    fun `should not call the location engine resolution recalculation callback if this trackable didn't have a resolution`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.resolutions.clear()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            recalculateResolutionCallbackFunction.invoke()
        }
    }

    @Test
    fun `should remove the trackable resolution requests`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.requests[trackable.id] = mutableMapOf()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.requests).isEmpty()
    }

    @Test
    fun `should remove the trackable last sent enhanced location`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.lastSentEnhancedLocations[trackable.id] = anyLocation()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.lastSentEnhancedLocations).isEmpty()
    }

    @Test
    fun `should remove the trackable last sent raw location`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.lastSentRawLocations[trackable.id] = anyLocation()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.lastSentRawLocations).isEmpty()
    }

    @Test
    fun `should remove the trackable last channel connection state change`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.lastChannelConnectionStateChanges[trackable.id] =
            ConnectionStateChange(ConnectionState.OFFLINE, null)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.lastChannelConnectionStateChanges).isEmpty()
    }

    @Test
    fun `should update the trackables in the publisher`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        verify(exactly = 1) {
            publisherInteractor.updateTrackables(updatedProperties)
        }
    }

    @Test
    fun `should update the trackable state flows in the publisher`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.updateTrackableStateFlows(updatedProperties)
        }
    }

    @Test
    fun `should notify the resolution policy that a trackable was removed`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.notifyResolutionPolicyThatTrackableWasRemoved(trackable)
        }
    }

    @Test
    fun `should remove all saved subscribers for the removed trackable`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.removeAllSubscribers(trackable, updatedProperties)
        }
    }

    @Test
    fun `should clear all skipped enhanced locations for the removed trackable`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.skippedEnhancedLocations.add(trackable.id, anyLocation())

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.skippedEnhancedLocations.toList(trackable.id)).isEmpty()
    }

    @Test
    fun `should clear all skipped raw locations for the removed trackable`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.skippedRawLocations.add(trackable.id, anyLocation())

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.skippedRawLocations.toList(trackable.id)).isEmpty()
    }

    @Test
    fun `should clear the enhanced locations publishing state for the removed trackable`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.enhancedLocationsPublishingState.markMessageAsPending(trackable.id)
        val locationUpdateEvent =
            EnhancedLocationUpdate(anyLocation(), emptyList(), emptyList(), LocationUpdateType.ACTUAL)
        initialProperties.enhancedLocationsPublishingState.addToWaiting(trackable.id, locationUpdateEvent)
        initialProperties.enhancedLocationsPublishingState.maxOutRetryCount(trackable.id)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.enhancedLocationsPublishingState.hasPendingMessage(trackable.id)).isFalse()
        assertThat(updatedProperties.enhancedLocationsPublishingState.getNextWaiting(trackable.id)).isNull()
        assertThat(updatedProperties.enhancedLocationsPublishingState.shouldRetryPublishing(trackable.id)).isTrue()
    }

    @Test
    fun `should clear the raw locations publishing state for the removed trackable`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.rawLocationsPublishingState.markMessageAsPending(trackable.id)
        val locationUpdateEvent = LocationUpdate(anyLocation(), emptyList())
        initialProperties.rawLocationsPublishingState.addToWaiting(trackable.id, locationUpdateEvent)
        initialProperties.rawLocationsPublishingState.maxOutRetryCount(trackable.id)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.rawLocationsPublishingState.hasPendingMessage(trackable.id)).isFalse()
        assertThat(updatedProperties.rawLocationsPublishingState.getNextWaiting(trackable.id)).isNull()
        assertThat(updatedProperties.rawLocationsPublishingState.shouldRetryPublishing(trackable.id)).isTrue()
    }

    fun `should remove the trackable from the trackable removal guard`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackableStateFlows[trackable.id] = MutableStateFlow(TrackableState.Offline())
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.trackableRemovalGuard.isMarkedForRemoval(trackable)).isFalse()
    }

    @Test
    fun `should always call the result callback with a success`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        // then
        verify(exactly = 1) {
            resultCallbackFunction(Result.success(Unit))
        }
    }

    @Test
    fun `should clear the active trackable if the removed trackable was the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = trackable

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.active).isNull()
    }

    @Test
    fun `should remove the current destination if the removed trackable was the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = trackable

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.removeCurrentDestination(updatedProperties)
        }
    }

    @Test
    fun `should notify the Resolution Policy that there is no active trackable the removed trackable was the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = trackable

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
        }
    }

    @Test
    fun `should not clear the active trackable if the removed trackable was not the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = otherTrackable

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.active).isEqualTo(otherTrackable)
    }

    @Test
    fun `should not remove the current destination if the removed trackable was not the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = otherTrackable

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.removeCurrentDestination(updatedProperties)
        }
    }

    @Test
    fun `should not notify the Resolution Policy that there is no active trackable if the removed trackable was not the active one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.active = otherTrackable

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
        }
    }

    @Test
    fun `should stop location updates if the removed trackable was the last one and the publisher is currently tracking`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackables.remove(otherTrackable)
        initialProperties.isTracking = true

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.stopLocationUpdates(updatedProperties)
        }
    }

    @Test
    fun `should not stop location updates if the removed trackable was not the last one`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.isTracking = true

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.stopLocationUpdates(updatedProperties)
        }
    }

    @Test
    fun `should not stop location updates if the publisher is not currently tracking`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackables.remove(otherTrackable)
        initialProperties.isTracking = false

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.stopLocationUpdates(updatedProperties)
        }
    }

    @Test
    fun `should stop ably and post StoppingConnectionFinished work if the removed trackable was the last one`() =
        runTest {
            // given
            val initialProperties = createPublisherPropertiesWithMultipleTrackables()
            initialProperties.trackables.remove(otherTrackable)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
            asyncWorks.executeAll()

            // then
            assertThat(asyncWorks).hasSize(1)
            assertThat(postedWorks).hasSize(1)
            val postedWorker = postedWorks.first()

            coVerify(exactly = 1) {
                ably.stopConnection()
            }

            assertThat(postedWorker).isEqualTo(WorkerSpecification.StoppingConnectionFinished)
        }

    private fun createPublisherPropertiesWithMultipleTrackables(): PublisherProperties {
        val properties = createPublisherProperties()
        properties.trackables.add(trackable)
        properties.trackables.add(otherTrackable)
        return properties
    }
}
