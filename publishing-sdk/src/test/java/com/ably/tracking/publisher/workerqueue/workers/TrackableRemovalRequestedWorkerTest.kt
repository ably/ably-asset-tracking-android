package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class TrackableRemovalRequestedWorkerTest {

    private val trackable = Trackable("test-trackable")
    private val otherTrackable = Trackable("other-trackable")
    private val ably: Ably = mockk {
        coEvery { stopConnection() } returns Result.success(Unit)
    }

    private val worker = TrackableRemovalRequestedWorker(trackable, ably, Result.success(Unit))

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post TrackableRemovalSuccessWork if result is successful`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        val removeTrackableCallbackFunction: ResultCallbackFunction<Boolean> = mockk(relaxed = true)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable, removeTrackableCallbackFunction)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).hasSize(1)

        val posted = postedWorks[0] as WorkerSpecification.TrackableRemovalSuccess
        assertThat(posted.trackable).isEqualTo(trackable)
        assertThat(posted.result.isSuccess).isTrue()
    }

    @Test
    fun `should mark removal failure if result is failure`() {
        // given
        val worker = prepareWorkerWithResult(Result.failure(Exception()))

        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        val removeTrackableCallbackFunction: ResultCallbackFunction<Boolean> = mockk(relaxed = true)
        initialProperties.trackableRemovalGuard.markForRemoval(trackable, removeTrackableCallbackFunction)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).hasSize(1)

        val posted = postedWorks[0] as WorkerSpecification.TrackableRemovalSuccess
        assertThat(posted.trackable).isEqualTo(trackable)
        assertThat(posted.result.isSuccess).isFalse()
    }

    @Test
    fun `should stop ably connection after removing last trackable`() = runTest {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        initialProperties.trackables.clear()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        asyncWorks.executeAll()

        // then
        assertThat(asyncWorks).hasSize(1)
        assertThat(postedWorks).hasSize(2)

        assertThat(updatedProperties.state).isEqualTo(PublisherState.DISCONNECTING)
        coVerify { ably.stopConnection() }
        assertThat(postedWorks[1]).isEqualTo(WorkerSpecification.StoppingConnectionFinished)
    }

    private fun prepareWorkerWithResult(result: Result<Unit>) =
        TrackableRemovalRequestedWorker(trackable, ably, result)

    private fun createPublisherPropertiesWithMultipleTrackables(): PublisherProperties {
        val properties = createPublisherProperties()
        properties.trackables.add(trackable)
        properties.trackables.add(otherTrackable)
        return properties
    }
}
