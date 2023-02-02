package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnterPresenceWorkerTest {

    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }
    private val trackable = Trackable("testtrackable")

    private lateinit var worker: EnterPresenceWorker

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should not post enter presence success if no trackable`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            worker = EnterPresenceWorker(trackable, true)

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
    }

    @Test
    fun `should not post retry enter presence if no trackable and enter presence failed`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            worker = EnterPresenceWorker(trackable, false)

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
    }

    fun `should not post enter presence success if trackable marked for removal`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}
            worker = EnterPresenceWorker(trackable, true)

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
    }

    @Test
    fun `should not post retry enter presence if trackable marked for removal and enter presence failed`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            initialProperties.trackableRemovalGuard.markForRemoval(trackable) {}
            worker = EnterPresenceWorker(trackable, false)

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
    }

    fun `should post enter presence success if presence enter on ably connect succeeded`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            worker = EnterPresenceWorker(trackable, true)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isEmpty()

            assertThat(postedWorks).hasSize(1)
            val postedWork = postedWorks[0] as WorkerSpecification.EnterPresenceSuccess
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }
    }

    @Test
    fun `should post retry enter presence if presence enter on ably connect failed`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            worker = EnterPresenceWorker(trackable, false)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isEmpty()

            assertThat(postedWorks).hasSize(1)
            val postedWork = postedWorks[0] as WorkerSpecification.RetryEnterPresence
            assertThat(postedWork.trackable).isEqualTo(trackable)
        }
    }
}
