package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockDisconnect
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class ConnectionReadyWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>> = mockk(relaxed = true)
    private val ably: Ably = mockk {
        every { subscribeForChannelStateChange(trackable.id, any()) } just runs
    }
    private val hooks: DefaultCorePublisher.Hooks = mockk {
        every { trackables } returns null
    }
    private val publisherInteractor = mockk<PublisherInteractor> {
        every { startLocationUpdates(any()) } just runs
        every { updateTrackables(any()) } just runs
        every { updateTrackableStateFlows(any()) } just runs
        every { resolveResolution(any(), any()) } just runs
    }
    private val connectionStateChangeListener: (ConnectionStateChange) -> Unit = {}
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    private val worker = createWorker(isSubscribedToPresence = true)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post no other work when is subscribed to and entered presence`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableEnteredPresenceFlags[trackable.id] = true

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
    }

    @Test
    fun `should post RetrySubscribeToPresence work when is not subscribed to presence`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableEnteredPresenceFlags[trackable.id] = true
        val worker = createWorker(isSubscribedToPresence = false)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).hasSize(1)

        val postedWork = postedWorks.first() as WorkerSpecification.RetrySubscribeToPresence
        assertThat(postedWork.trackable).isEqualTo(trackable)
        assertThat(postedWork.presenceUpdateListener).isEqualTo(presenceUpdateListener)
    }

    @Test
    fun `should post RetryEnterPresence work when is not entered presence`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableEnteredPresenceFlags.remove(trackable.id)
        val worker = createWorker(isSubscribedToPresence = true)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).hasSize(1)

        val postedWork = postedWorks.first() as WorkerSpecification.RetryEnterPresence
        assertThat(postedWork.trackable).isEqualTo(trackable)
    }

    @Test
    fun `should subscribe to Ably channel state updates when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

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
            ably.subscribeForChannelStateChange(trackable.id, any())
        }
    }

    @Test
    fun `should start location updates if is not already tracking when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.isTracking = false

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
            publisherInteractor.startLocationUpdates(any())
        }
    }

    @Test
    fun `should not start location updates if is already tracking when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.isTracking = true

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
            publisherInteractor.startLocationUpdates(any())
        }
    }

    @Test
    fun `should add the trackable to the tracked trackables when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackables).contains(trackable)
    }

    @Test
    fun `should update the tracked trackables when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

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
            publisherInteractor.updateTrackables(any())
        }
    }

    @Test
    fun `should calculate a resolution for the added trackable when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

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
            publisherInteractor.resolveResolution(trackable, any())
        }
    }

    @Test
    fun `should set a state flow for the trackable when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableStateFlows[trackable.id]).isNotNull()
    }

    @Test
    fun `should update state flows when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

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
            publisherInteractor.updateTrackableStateFlows(any())
        }
    }

    @Test
    fun `should set the initial trackable state to offline when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableStates[trackable.id]).isInstanceOf(TrackableState.Offline::class.java)
    }

    @Test
    fun `should call the adding trackable callback with a success when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify { resultCallbackFunction.invoke(match { it.isSuccess }) }
    }

    @Test
    fun `should finish adding the trackable with a success when executing normally`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        val addTrackableCallbackFunction: AddTrackableCallbackFunction = mockk(relaxed = true)
        initialProperties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, addTrackableCallbackFunction)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify { addTrackableCallbackFunction.invoke(match { it.isSuccess }) }
    }

    @Test
    fun `should return only async result when trackable removal was requested`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).hasSize(1)
        assertThat(postedWorks).isEmpty()
    }

    @Test
    fun `should return trackable removal work result when trackable removal was requested`() = runTest {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        ably.mockDisconnect(trackable.id)

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

        val postedWork = postedWorks.first() as WorkerSpecification.TrackableRemovalRequested

        assertThat(postedWork.trackable).isEqualTo(trackable)
        assertThat(postedWork.callbackFunction).isEqualTo(resultCallbackFunction)
    }

    @Test
    fun `should disconnect from Ably when trackable removal was requested`() = runTest {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        ably.mockDisconnect(trackable.id)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        coVerify(exactly = 1) {
            ably.disconnect(trackable.id, any())
        }
    }

    @Test
    fun `should not perform any of the normal operations when trackable removal was requested`() = runTest {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}
        initialProperties.duplicateTrackableGuard.startAddingTrackable(trackable)

        ably.mockDisconnect(trackable.id)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        assertThat(updatedProperties.trackables).doesNotContain(trackable)
        assertThat(updatedProperties.trackableStates[trackable.id]).isNull()
        assertThat(updatedProperties.trackableStateFlows[trackable.id]).isNull()
        assertThat(updatedProperties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable)).isTrue()

        verify(exactly = 0) {
            ably.subscribeForChannelStateChange(trackable.id, any())
            publisherInteractor.startLocationUpdates(any())
            publisherInteractor.updateTrackables(any())
            publisherInteractor.resolveResolution(trackable, any())
            publisherInteractor.updateTrackableStateFlows(any())
            resultCallbackFunction.invoke(any())
        }
    }

    private fun createDefaultPublisherProperties(trackable: Trackable) = createPublisherProperties().also {
        it.trackableEnteredPresenceFlags[trackable.id] = true
    }

    private fun createWorker(isSubscribedToPresence: Boolean) =
        ConnectionReadyWorker(
            trackable,
            resultCallbackFunction,
            ably,
            hooks,
            publisherInteractor,
            connectionStateChangeListener,
            isSubscribedToPresence,
            presenceUpdateListener,
        )
}
