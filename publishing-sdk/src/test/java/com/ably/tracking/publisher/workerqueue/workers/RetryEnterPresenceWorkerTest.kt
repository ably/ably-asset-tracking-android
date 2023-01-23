package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockConnectFailure
import com.ably.tracking.test.common.mockConnectSuccess
import com.google.common.truth.Truth
import io.mockk.coEvery
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
    fun `should not post any work when connection succeeded`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectSuccess(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            Truth.assertThat(postedWorks).isEmpty()
        }
    }

    @Test
    fun `should post RetryEnterPresence work after delay when connection failed with a non-fatal error`() {
        runTest(context = UnconfinedTestDispatcher()) {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectFailure(trackable.id, isFatal = false)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.launchAll(this)

            Truth.assertThat(postedWorks).isEmpty()

            advanceUntilIdle()

            Truth.assertThat(postedWorks)
                .contains(WorkerSpecification.RetryEnterPresence(trackable))
        }
    }
}
