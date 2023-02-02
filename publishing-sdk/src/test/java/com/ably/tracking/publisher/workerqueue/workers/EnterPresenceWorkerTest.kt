package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnterPresenceWorkerTest {

    private val trackable = Trackable("testtrackable")

    private val worker = EnterPresenceWorker(trackable)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should not post enter presence success if no trackable`() {
        runTest {
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
    }

    @Test
    fun `should not post retry enter presence if no trackable and enter presence failed`() {
        runTest {
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
    }

    @Test
    fun `should not post enter presence success if trackable marked for removal`() {
        runTest {
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
    }

    @Test
    fun `should not post retry enter presence if trackable marked for removal and enter presence failed`() {
        runTest {
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
    }

    @Test
    fun `should post retry enter presence if presence enter on ably connect failed`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)

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
