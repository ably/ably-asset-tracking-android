package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AddTrackableWorkerTest {
    private lateinit var worker: AddTrackableWorker

    // dependencies
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>()
    private val ably = mockk<Ably>(relaxed = true)
    private val trackable = Trackable("testtrackable")

    @Before
    fun setUp() {
        worker = AddTrackableWorker(trackable, resultCallbackFunction, ably)
    }

    @Test
    fun `doWork returns asyncWork when trackable is not added and not being added`() {
        // given
        val publisherProperties = FakeProperties(FakeDuplicateGuard(false))
        // when
        val result = worker.doWork(publisherProperties)
        // then
        Assert.assertNotNull(result.asyncWork)
    }

    @Test
    fun `doWork triggers dublicateTrackableGuard startAddingTrackable when adding trackable in clean state`() {
        // given
        // need to use spy to use verify
        val guard = spyk(FakeDuplicateGuard(false))
        val publisherProperties = FakeProperties(guard)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            guard.startAddingTrackable(trackable)
        }
    }

    @Test
    fun `doWork returns empty result if trackable is being added`() {
        // given
        val publisherProperties = FakeProperties(FakeDuplicateGuard(true))
        // when
        val result = worker.doWork(publisherProperties)
        // then
        Assert.assertNull(result.asyncWork)
        Assert.assertNull(result.syncWorkResult)
    }

    @Test
    fun `doWork triggers dublicateTrackableGuard saveDuplicateAddHandler when adding trackable that is being added`() {
        // given
        val guard = spyk(FakeDuplicateGuard(true))
        val publisherProperties = FakeProperties(guard)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            guard.saveDuplicateAddHandler(trackable, resultCallbackFunction)
        }
    }

    @Test
    fun `doWork returns AlreadyIn result if trackable is already added`() {
        // given
        val publisherProperties = FakeProperties(FakeDuplicateGuard(false))
        publisherProperties.trackables.add(trackable)
        publisherProperties.trackableStateFlows[trackable.id] = MutableStateFlow(TrackableState.Online)
        // when
        val result = worker.doWork(publisherProperties)
        // then
        Assert.assertNull(result.asyncWork)
        Assert.assertTrue(result.syncWorkResult is AddTrackableWorkResult.AlreadyIn)
        //also make sure it has the right content
        val alreadyIn = result.syncWorkResult as AddTrackableWorkResult.AlreadyIn
        Assert.assertTrue(alreadyIn.callbackFunction == resultCallbackFunction)
        Assert.assertTrue(alreadyIn.trackableStateFlow == publisherProperties.trackableStateFlows[trackable.id])
    }

}
