package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class TrackableRemovalRequestedWorkerTest {

    private val trackable = Trackable("test-trackable")
    private val otherTrackable = Trackable("other-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val ably: Ably = mockk {
        coEvery { stopConnection() } returns Result.success(Unit)
    }

    private val worker = TrackableRemovalRequestedWorker(trackable, resultCallbackFunction, ably, Result.success(Unit))

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should always call the add trackable callback with a trackable removal requested exception`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        val addTrackableResultSlot = captureResultCallbackFunctionResult()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        val addTrackableResult = addTrackableResultSlot.captured
        assertThat(addTrackableResult.exceptionOrNull()).isInstanceOf(RemoveTrackableRequestedException::class.java)
    }

    @Test
    fun `should always finish adding the trackable with a trackable removal requested exception`() {
        // given
        val initialProperties = createPublisherPropertiesWithMultipleTrackables()
        val addTrackableCallbackFunction: AddTrackableCallbackFunction = mockk(relaxed = true)
        initialProperties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, addTrackableCallbackFunction)

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
            addTrackableCallbackFunction.invoke(match {
                it.exceptionOrNull() is RemoveTrackableRequestedException
            })
        }
    }

    @Test
    fun `should mark removal success if result is successful`() {
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
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            removeTrackableCallbackFunction.invoke(match {
                it.getOrNull() == true
            })
        }
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
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            removeTrackableCallbackFunction.invoke(match {
                it.exceptionOrNull() != null
            })
        }
    }

    @Test
    fun `should stop ably connection after removing last trackable`() = runBlockingTest {
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
        assertThat(postedWorks).hasSize(1)

        assertThat(updatedProperties.state).isEqualTo(PublisherState.DISCONNECTING)
        coVerify { ably.stopConnection() }
        assertThat(postedWorks.first()).isEqualTo(WorkerSpecification.StoppingConnectionFinished)
    }

    private fun prepareWorkerWithResult(result: Result<Unit>) =
        TrackableRemovalRequestedWorker(trackable, resultCallbackFunction, ably, result)

    private fun captureResultCallbackFunctionResult(): CapturingSlot<Result<StateFlow<TrackableState>>> {
        val resultCallbackFunctionResult = slot<Result<StateFlow<TrackableState>>>()
        every { resultCallbackFunction.invoke(capture(resultCallbackFunctionResult)) } just runs
        return resultCallbackFunctionResult
    }

    private fun createPublisherPropertiesWithMultipleTrackables(): PublisherProperties {
        val properties = createPublisherProperties()
        properties.trackables.add(trackable)
        properties.trackables.add(otherTrackable)
        return properties
    }
}
