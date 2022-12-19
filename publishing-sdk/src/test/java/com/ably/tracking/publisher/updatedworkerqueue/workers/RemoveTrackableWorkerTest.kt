package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class RemoveTrackableWorkerTest {
    private val trackable = Trackable("testtrackable")
    private val ably: Ably = mockk()
    private val callbackFunction: ResultCallbackFunction<Boolean> = mockk(relaxed = true)

    private val worker = RemoveTrackableWorker(trackable, ably, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `when removing trackable that is not present should invoke callback with Success(false)`() = runBlockingTest {
        // given
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // then
        assertThat(asyncWorks.size).isEqualTo(1)
        assertThat(postedWorks).isEmpty()

        verify {
            callbackFunction(match { it.getOrNull() == false })
        }
    }

    @Test
    fun `when removing trackable that is currently added should add it to trackableRemovalGuard`() =
        runBlockingTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.startAddingTrackable(trackable)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isEmpty()
            assertThat(postedWorks).isEmpty()
            assertThat(initialProperties.trackableRemovalGuard.isMarkedForRemoval(trackable))
                .isEqualTo(true)
        }

    @Test
    fun `when removing trackable that is present succeeded should invoke callback with exception`() =
        runBlockingTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            coEvery { ably.disconnect(trackable.id, any()) } returns Result.success(Unit)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
            asyncWorks.executeAll()

            // then
            assertThat(asyncWorks.size).isEqualTo(1)
            assertThat(postedWorks.size).isEqualTo(1)
            assertThat(postedWorks[0])
                .isInstanceOf(WorkerSpecification.DisconnectSuccess::class.java)
        }

    @Test
    fun `when removing trackable that is present fails should invoke callback with exception`() =
        runBlockingTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            coEvery { ably.disconnect(trackable.id, any()) } returns Result.failure(RuntimeException("testException"))

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
            asyncWorks.executeAll()

            // then
            assertThat(asyncWorks.size).isEqualTo(1)
            assertThat(postedWorks).isEmpty()

            verify {
                callbackFunction(match { it.exceptionOrNull() is RuntimeException })
            }
        }
}
