package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockDisconnect
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    fun `when removing trackable that is not present should invoke callback with Success(false)`() = runTest {
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
        runTest {
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
    fun `when removing trackable that is present should post disconnect success worker`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            ably.mockDisconnect(trackable.id)

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
}
