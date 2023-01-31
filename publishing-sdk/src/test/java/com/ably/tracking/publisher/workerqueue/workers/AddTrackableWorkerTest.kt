package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockConnectFailure
import com.ably.tracking.test.common.mockConnectSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTrackableWorkerTest {

    private val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>> =
        mockk(relaxed = true)
    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }
    private val hooks: DefaultCorePublisher.Hooks = mockk {
        every { trackables } returns null
    }
    private val publisherInteractor = mockk<PublisherInteractor> {
        every { startLocationUpdates(any()) } just runs
        every { updateTrackables(any()) } just runs
        every { updateTrackableStateFlows(any()) } just runs
        every { resolveResolution(any(), any()) } just runs
    }
    private val trackable = Trackable("testtrackable")

    private val worker = AddTrackableWorker(trackable, resultCallbackFunction, {}, {}, ably, publisherInteractor, hooks)

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
            .isFalse()
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

        updatedProperties.duplicateTrackableGuard.finishAddingTrackable(
            trackable,
            addTrackableResult
        )

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
        initialProperties.trackableStateFlows[trackable.id] =
            MutableStateFlow(TrackableState.Offline())

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
            assertThat(asyncWorks).isNotEmpty()

            val postedWorkerSpecification = postedWorks[0] as WorkerSpecification.ConnectionCreated
            assertThat(postedWorkerSpecification.trackable).isEqualTo(trackable)
        }
    }

    @Test
    fun `should post ConnectionCreated work when connection failed with a non-fatal error`() {
        runTest {
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
            asyncWorks.executeAll()
            assertThat(asyncWorks).isNotEmpty()

            val postedWorkerSpecification = postedWorks[0] as WorkerSpecification.ConnectionCreated
            assertThat(postedWorkerSpecification.trackable).isEqualTo(trackable)
        }
    }

    @Test
    fun `should fail to add a trackable when connection failed with fatal error`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectFailure(trackable.id, isFatal = true)

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

    @Test
    fun `should add the trackable to the tracked trackables when executing normally`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectSuccess(trackable.id)

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            assertThat(updatedProperties.trackables).contains(trackable)
        }
    }

    @Test
    fun `should update the tracked trackables when executing normally`() {
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
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            verify(exactly = 1) {
                publisherInteractor.updateTrackables(any())
            }
        }
    }

    @Test
    fun `should calculate a resolution for the added trackable when executing normally`() {
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
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            verify(exactly = 1) {
                publisherInteractor.resolveResolution(trackable, any())
            }
        }
    }

    @Test
    fun `should set a state flow for the trackable when executing normally`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectSuccess(trackable.id)

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            assertThat(updatedProperties.trackableStateFlows[trackable.id]).isNotNull()
        }
    }

    @Test
    fun `should update state flows when executing normally`() {
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
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            verify(exactly = 1) {
                publisherInteractor.updateTrackableStateFlows(any())
            }
        }
    }

    @Test
    fun `should set the initial trackable state to offline when executing normally`() {
        runTest {
            // given
            val initialProperties = createPublisherProperties()
            initialProperties.duplicateTrackableGuard.clear(trackable)
            ably.mockConnectSuccess(trackable.id)

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            assertThat(updatedProperties.trackableStates[trackable.id]).isInstanceOf(TrackableState.Offline::class.java)
        }
    }

    @Test
    fun `should call the adding trackable callback with a success when executing normally`() {
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
            assertThat(asyncWorks).isNotEmpty()
            assertThat(postedWorks).isEmpty()

            verify { resultCallbackFunction.invoke(match { it.isSuccess }) }
        }
    }
}
