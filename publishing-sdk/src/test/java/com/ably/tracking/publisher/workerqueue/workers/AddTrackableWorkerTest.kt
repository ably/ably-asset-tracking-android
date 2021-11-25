package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import io.mockk.mockk
import kotlinx.coroutines.flow.StateFlow
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

    }
}
