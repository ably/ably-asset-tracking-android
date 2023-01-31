package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockDisconnect
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class ConnectionCreatedWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val ably = mockk<Ably>(relaxed = true)
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    private val worker =
        ConnectionCreatedWorker(trackable, true, resultCallbackFunction, ably, null, presenceUpdateListener) {}

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
            assertThat(postedWork.callbackFunction).isEqualTo(resultCallbackFunction)
            assertThat(postedWork.presenceUpdateListener).isEqualTo(presenceUpdateListener)
            assertThat(postedWork.isSubscribedToPresence).isTrue()
        }

    @Test
    fun `should mark entered trackable presence as true`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(updatedProperties.trackableEnteredPresenceFlags[trackable.id]).isTrue()
        }

    @Test
    fun `should not mark entered trackable presence if enteredPresence is false`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            val worker = ConnectionCreatedWorker(trackable, false, resultCallbackFunction, ably, null, presenceUpdateListener) {}

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(updatedProperties.trackableEnteredPresenceFlags[trackable.id]).isNull()
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
            assertThat(postedWork.callbackFunction).isEqualTo(resultCallbackFunction)
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
        assertThat(postedWork.callbackFunction).isEqualTo(resultCallbackFunction)
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
}
