package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockConnectFailure
import com.ably.tracking.test.common.mockConnectSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AddTrackableWorkerTest {

    private val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>> = mockk(relaxed = true)
    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }
    private val trackable = Trackable("testtrackable")

    private val worker = AddTrackableWorker(trackable, resultCallbackFunction, {}, {}, ably)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should start adding a trackable when adding a trackable that is not added and not being added`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.duplicateTrackableGuard.clear(trackable)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isNotEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(updatedProperties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable))
            .isTrue()
    }

    @Test
    fun `should save the trackable callback function when adding a trackable that is being added`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.duplicateTrackableGuard.startAddingTrackable(trackable)
        val addTrackableResult = Result.success(MutableStateFlow(TrackableState.Offline()))

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        updatedProperties.duplicateTrackableGuard.finishAddingTrackable(trackable, addTrackableResult)

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            resultCallbackFunction.invoke(addTrackableResult)
        }
    }

    @Test
    fun `should call callback with success when adding a trackable that is already added`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.trackables.add(trackable)
        initialProperties.trackableStateFlows[trackable.id] = MutableStateFlow(TrackableState.Offline())

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            resultCallbackFunction(
                match {
                    it.getOrNull() == updatedProperties.trackableStateFlows[trackable.id]
                }
            )
        }
    }

    // async work tests
    @Test
    fun `should post ConnectionCreated work when connection was successful`() {
        runBlocking {
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
            assertThat(asyncWorks).isNotEmpty()

            val postedWorkerSpecification = postedWorks[0] as WorkerSpecification.ConnectionCreated
            assertThat(postedWorkerSpecification.trackable).isEqualTo(trackable)
            assertThat(postedWorkerSpecification.callbackFunction).isEqualTo(resultCallbackFunction)
        }
    }

    @Test
    fun `should fail to add a trackable when connection failed`() {
        runBlocking {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectFailure(trackable.id)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()
            assertThat(asyncWorks).isNotEmpty()

            val postedWorkerSpecification = postedWorks[0] as WorkerSpecification.AddTrackableFailed
            assertThat(postedWorkerSpecification.trackable).isEqualTo(trackable)
            assertThat(postedWorkerSpecification.callbackFunction).isEqualTo(resultCallbackFunction)
        }
    }
}
