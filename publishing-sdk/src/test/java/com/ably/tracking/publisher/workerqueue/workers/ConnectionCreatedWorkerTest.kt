package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockDisconnect
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class ConnectionCreatedWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val ably = mockk<Ably>(relaxed = true)
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    private val worker =
        ConnectionCreatedWorker(trackable, ably, null, presenceUpdateListener) {}

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should return presence success result when executing normally and presence enter was successful`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            ably.mockSubscribeToPresenceSuccess(trackable.id)

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

            val postedWork = postedWorks.first() as WorkerSpecification.ConnectionReady
            assertThat(postedWork.trackable).isEqualTo(trackable)
            assertThat(postedWork.presenceUpdateListener).isEqualTo(presenceUpdateListener)
            assertThat(postedWork.isSubscribedToPresence).isTrue()
        }

    @Test
    fun `should return presence failure result when executing normally and presence enter failed`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            ably.mockSubscribeToPresenceError(trackable.id)

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

            val postedWork = postedWorks.first() as WorkerSpecification.ConnectionReady
            assertThat(postedWork.trackable).isEqualTo(trackable)
            assertThat(postedWork.presenceUpdateListener).isEqualTo(presenceUpdateListener)
            assertThat(postedWork.isSubscribedToPresence).isFalse()
        }

    @Test
    fun `should post TrackableRemovalRequested work when trackable removal was requested`() = runTest {
        // given
        val initialProperties = createPublisherProperties()
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
        val initialProperties = createPublisherProperties()
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

        coVerify(exactly = 1) {
            ably.disconnect(trackable.id, any())
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
        assertThat(postedWork.errorInformation.message).isEqualTo("Unexpected error on connection created: java.lang.Exception: Foo")
    }

    @Test
    fun `should post trackable removal requested on unexpected async error if marked for removal`() = runTest {
        // Given
        val initialProperties = createPublisherProperties()
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // when
        worker.onUnexpectedAsyncError(
            Exception("Foo"),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).hasSize(1)
        assertThat(postedWorks).hasSize(2)

        val postedWork = postedWorks.first() as WorkerSpecification.TrackableRemovalRequested
        assertThat(postedWork.trackable).isEqualTo(trackable)
        val postedWork2 = postedWorks.get(1) as WorkerSpecification.TrackableRemovalRequested
        assertThat(postedWork2.trackable).isEqualTo(trackable)
    }

    @Test
    fun `should post connection ready if async error and not removal requested`() = runTest {
        // Given
        val initialProperties = createPublisherProperties()

        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // when
        worker.onUnexpectedAsyncError(
            Exception("Foo"),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).hasSize(1)
        assertThat(postedWorks).hasSize(1)

        val postedWork = postedWorks.first() as WorkerSpecification.FailTrackable
        assertThat(postedWork.trackable).isEqualTo(trackable)
        assertThat(postedWork.errorInformation.message).isEqualTo("Unexpected async error on connection created: java.lang.Exception: Foo")
    }
}
