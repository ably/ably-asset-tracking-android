package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockEnterPresenceFailure
import com.ably.tracking.test.common.mockEnterPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RetryEnterPresenceWorkerTest {

    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }
    private val trackable = Trackable("testtrackable")

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
    fun `should post RetryEnterPresence work when connection failed with a fatal error`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            initialProperties.trackables.add(trackable)
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
}