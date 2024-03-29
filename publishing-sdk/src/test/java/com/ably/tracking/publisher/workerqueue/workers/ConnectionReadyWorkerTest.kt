package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherState
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
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class ConnectionReadyWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val ably: Ably = mockk {
        every { subscribeForChannelStateChange(trackable.id, any()) } just runs
    }
    private val publisherInteractor = mockk<PublisherInteractor> {
        every { startLocationUpdates(any()) } just runs
    }
    private val connectionStateChangeListener: (ConnectionStateChange) -> Unit = {}

    private val worker = createWorker()

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should set publisher state to connected if currently connecting`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.state = PublisherState.CONNECTING

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.state).isEqualTo(PublisherState.CONNECTED)
    }

    @Test
    fun `should not set publisher state to connected if not currently connecting`() {
        // given
        val initialProperties = createDefaultPublisherProperties(trackable)
        initialProperties.state = PublisherState.IDLE

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.state).isEqualTo(PublisherState.IDLE)
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

        ably.mockDisconnect(trackable.id)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        verify(exactly = 0) {
            publisherInteractor.startLocationUpdates(any())
        }
    }

    @Test
    fun `should fail trackable on unexpected error`() = runTest {
        // when
        worker.onUnexpectedError(
            Exception("Foo"),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // then
        assertThat(asyncWorks).hasSize(0)
        assertThat(postedWorks).hasSize(1)

        val postedWork = postedWorks.first() as WorkerSpecification.FailTrackable
        assertThat(postedWork.trackable).isEqualTo(trackable)
        assertThat(postedWork.errorInformation.message).isEqualTo("Unexpected error on connection ready: java.lang.Exception: Foo")
    }

    @Test
    fun `should post trackable removal requested on unexpected async error`() = runTest {
        // Given
        val initialProperties = createPublisherProperties()
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        ably.mockDisconnect(trackable.id)

        // when
        worker.onUnexpectedAsyncError(
            Exception("Foo"),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).hasSize(0)
        assertThat(postedWorks).hasSize(1)

        val postedWork = postedWorks.first() as WorkerSpecification.TrackableRemovalRequested
        assertThat(postedWork.trackable).isEqualTo(trackable)
    }

    private fun createDefaultPublisherProperties(trackable: Trackable) = createPublisherProperties().also {
        it.trackableEnteredPresenceFlags[trackable.id] = true
        it.trackables.add(trackable)
    }

    private fun createWorker() =
        ConnectionReadyWorker(
            trackable,
            ably,
            publisherInteractor,
            connectionStateChangeListener,
        )
}
