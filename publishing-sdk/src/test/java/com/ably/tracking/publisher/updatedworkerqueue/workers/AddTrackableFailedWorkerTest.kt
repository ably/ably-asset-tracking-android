package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.updatedworkerqueue.workers.AddTrackableFailedWorker
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AddTrackableFailedWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val exception = Exception("test-exception")
    private val ably: Ably = mockk {
        coEvery { stopConnection() } returns Result.success(Unit)
    }

    private val worker = AddTrackableFailedWorker(trackable, resultCallbackFunction, exception, true, ably)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call the adding trackable callback with a failure result`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(Trackable("tracked-trackable"))

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            resultCallbackFunction.invoke(Result.failure(exception))
        }
    }

    @Test
    fun `should finish adding the trackable with a failure`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.duplicateTrackableGuard.startAddingTrackable(trackable)
        initialProperties.trackables.add(Trackable("tracked-trackable"))

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable))
            .isFalse()
    }

    @Test
    fun `should finish removing the trackable with a success`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.duplicateTrackableGuard.startAddingTrackable(trackable)
        initialProperties.trackables.add(Trackable("tracked-trackable"))

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.trackableRemovalGuard.isMarkedForRemoval(trackable))
            .isFalse()
    }

    @Test
    fun `should post StoppingConnectionFinished work after performing async work`() = runBlockingTest {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.clear()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isNotEmpty()
        asyncWorks.executeAll()
        assertThat(postedWorks).isNotEmpty()

        val postedWorker = postedWorks.first()
        assertThat(postedWorker).isInstanceOf(WorkerSpecification.StoppingConnectionFinished::class.java)
    }
}