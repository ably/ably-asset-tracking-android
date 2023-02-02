package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockEnterPresenceFailure
import com.ably.tracking.test.common.mockEnterPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RetryEnterPresenceWorkerTest {

    private val trackable = Trackable("testtrackable")

    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }

    private val worker = RetryEnterPresenceWorker(trackable, ably)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post RetryEnterPresenceSuccess work when connection succeeded`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            initialProperties.duplicateTrackableGuard.clear(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceSuccess(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWork = postedWorks[0] as WorkerSpecification.RetryEnterPresenceSuccess
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }
    }

    @Test
    fun `should not attempt to enter presence when trackable is no longer present`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            coVerify(exactly = 0) {
                ably.connect(any(), any(), any(), any(), any())
            }
        }
    }

    @Test
    fun `should not attempt to enter presence when trackable is marked for removal`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            coVerify(exactly = 0) {
                ably.connect(any(), any(), any(), any(), any())
            }
        }
    }

    @Test
    fun `should post RetryEnterPresence work after delay when connection failed with a non-fatal error`() {
        runTest(context = UnconfinedTestDispatcher()) {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceFailure(trackable.id, isFatal = false)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.launchAll(this)

            assertThat(postedWorks).isEmpty()

            advanceUntilIdle()

            assertThat(postedWorks)
                .contains(WorkerSpecification.RetryEnterPresence(trackable))
        }
    }

    @Test
    fun `should post FailTrackable work when connection failed with a fatal error`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceFailure(trackable.id, isFatal = true)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWork = postedWorks[0] as WorkerSpecification.FailTrackable
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }
    }

    @Test
    fun `should post RetryEnterPresence work when connection failed with a fatal error with 91001 error code`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceChannelSuspendedException(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.launchAll(this)

            assertThat(postedWorks).isEmpty()

            advanceUntilIdle()

            assertThat(postedWorks)
                .contains(WorkerSpecification.RetryEnterPresence(trackable))
        }
    }

    private fun Ably.mockEnterPresenceChannelSuspendedException(trackableId: String) {
        coEvery {
            enterChannelPresence(trackableId, any())
        } returns Result.failure(channelSuspendedException())
    }

    // returns connection exception specific to enter presence on a suspended channel
    private fun channelSuspendedException() = ConnectionException(
        ErrorInformation(
            code = 91001,
            statusCode = 400,
            message = "Test",
            href = null,
            cause = null
        )
    )

    @Test
    fun `should post FailTrackable work when connection when channel transitions to failed`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackables.add(trackable)
            mockChannelStateChange(ConnectionState.FAILED)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWork = postedWorks[0] as WorkerSpecification.FailTrackable
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }
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
