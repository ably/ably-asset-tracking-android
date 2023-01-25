package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockDisconnect
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class RemoveTrackableWorkerTest {
    private val trackable = Trackable("testtrackable")
    private val ably: Ably = mockk()
    private val publisherInteractor: PublisherInteractor = mockk()
    private val callbackFunction: ResultCallbackFunction<Boolean> = mockk(relaxed = true)

    private val worker = RemoveTrackableWorker(trackable, ably, publisherInteractor, callbackFunction)

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
            every { publisherInteractor.setFinalTrackableState(any(), trackable.id, any()) } just runs
            every { publisherInteractor.updateTrackableStateFlows(any()) } just runs

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
    fun `when removing trackable that is present should immediately call the callback with a success`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            ably.mockDisconnect(trackable.id)
            every { publisherInteractor.setFinalTrackableState(any(), trackable.id, any()) } just runs
            every { publisherInteractor.updateTrackableStateFlows(any()) } just runs

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
            asyncWorks.executeAll()

            // then
            verify {
                callbackFunction(match { it.getOrNull() == true })
            }
        }

    @Test
    fun `when removing trackable that is present should immediately change trackable state to offline`() =
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.trackables.add(trackable)
            ably.mockDisconnect(trackable.id)
            every { publisherInteractor.setFinalTrackableState(any(), trackable.id, any()) } just runs
            every { publisherInteractor.updateTrackableStateFlows(any()) } just runs

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            verify(exactly = 1) {
                publisherInteractor.setFinalTrackableState(any(), trackable.id, match { it is TrackableState.Offline })
                publisherInteractor.updateTrackableStateFlows(any())
            }
        }
}
