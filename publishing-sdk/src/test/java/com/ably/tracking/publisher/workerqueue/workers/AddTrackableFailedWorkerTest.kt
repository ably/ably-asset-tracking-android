package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AddTrackableFailedWorkerTest {
    private lateinit var worker: AddTrackableFailedWorker
    private val trackable = Trackable("test-trackable")
    private val resultCallbackFunction = mockk<ResultCallbackFunction<StateFlow<TrackableState>>>(relaxed = true)
    private val exception = Exception("test-exception")
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)
    private val duplicateTrackableGuard = mockk<DuplicateTrackableGuard>(relaxed = true)
    private val trackableRemovalGuard = mockk<TrackableRemovalGuard>(relaxed = true)
    private val ably = mockk<Ably>(relaxed = true)

    @Before
    fun setUp() {
        worker = AddTrackableFailedWorker(trackable, resultCallbackFunction, exception, true, ably)
        every { publisherProperties.duplicateTrackableGuard } returns duplicateTrackableGuard
        every { publisherProperties.trackableRemovalGuard } returns trackableRemovalGuard
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should return an empty result`() {
        // given

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should call the adding trackable callback with a failure result`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resultCallbackFunction.invoke(Result.failure(exception))
        }
    }

    @Test
    fun `should finish adding the trackable with a failure`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            duplicateTrackableGuard.finishAddingTrackable(trackable, Result.failure(exception))
        }
    }

    @Test
    fun `should finish removing the trackable with a success`() {
        // given

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            trackableRemovalGuard.removeMarked(trackable, Result.success(true))
        }
    }
}
