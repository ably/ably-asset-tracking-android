package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class SubscribeToPresenceWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val ably = mockk<Ably>(relaxed = true)
    private val presenceUpdateListener: (PresenceMessage) -> Unit = {}

    private val worker = SubscribeToPresenceWorker(trackable, ably, null, presenceUpdateListener)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post no async works if the trackable is not in the added trackable set`() {
        // given
        val initialProperties = createPublisherProperties()

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
    fun `should post no async works if the trackable is marked for removal`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

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
    fun `should post no other works if the channel went to the failed state`() = runTest {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)
        mockChannelStateChange(ConnectionState.FAILED)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        assertThat(asyncWorks).hasSize(1)
        assertThat(postedWorks).isEmpty()
    }

    @Test
    fun `should not try to subscribe to presence if the channel went to the failed state`() = runTest {
        // given
        val initialProperties = createPublisherProperties()
        mockChannelStateChange(ConnectionState.FAILED)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        verify(exactly = 0) {
            ably.subscribeForPresenceMessages(trackable.id, any(), any<(Result<Unit>) -> Unit>())
        }
    }

    @Test
    fun `should post RetrySubscribeToPresenceSuccess if the channel went to the online state and subscribe to presence was successful`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockSubscribeToPresenceSuccess(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            asyncWorks.executeAll()

            // then
            assertThat(postedWorks).hasSize(1)
            val postedWork = postedWorks.first() as WorkerSpecification.SubscribeToPresenceSuccess
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }

    @Test
    fun `should return failure if the channel went to the online state but subscribe to presence has failed`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockSubscribeToPresenceError(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            asyncWorks.executeAll()

            // then
            assertThat(postedWorks).hasSize(1)
            val postedWork = postedWorks.first() as WorkerSpecification.SubscribeToPresence
            assertThat(postedWork.trackable).isEqualTo(trackable)
            assertThat(postedWork.presenceUpdateListener).isEqualTo(presenceUpdateListener)
        }

    private fun mockChannelStateChange(newState: ConnectionState) {
        every {
            runBlocking {
                ably.waitForChannelToAttach(trackable.id)
            }
        } returns when (newState) {
            ConnectionState.ONLINE -> Result.success(Unit)
            ConnectionState.FAILED -> Result.failure(ConnectionException(ErrorInformation("Channel attach failed")))
            ConnectionState.OFFLINE -> throw Exception("Not a valid result")
        }
    }
}
