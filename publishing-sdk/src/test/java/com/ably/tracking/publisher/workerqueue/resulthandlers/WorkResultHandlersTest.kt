package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerQueue
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.results.RemoveTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.RetrySubscribeToPresenceWorkResult
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Test

// Without it we're not allowed to check result handler type with `is`
@Suppress("INCOMPATIBLE_TYPES")
class WorkResultHandlersTest {
    private val workerFactory = mockk<WorkerFactory>(relaxed = true)
    private val workerQueue = mockk<WorkerQueue>(relaxed = true)
    private val addTrackableWorkResults = listOf(
        AddTrackableWorkResult.AlreadyIn(MutableStateFlow(TrackableState.Online), {}),
        AddTrackableWorkResult.Success(Trackable(""), {}, {}, {}),
        AddTrackableWorkResult.Fail(Trackable(""), null, {}),
    )
    private val connectionCreatedWorkResults = listOf(
        ConnectionCreatedWorkResult.RemovalRequested(Trackable(""), {}, Result.success(Unit)),
        ConnectionCreatedWorkResult.PresenceSuccess(Trackable(""), {}, {}, {}),
        ConnectionCreatedWorkResult.PresenceFail(Trackable(""), {}, {}, {}),
    )
    private val connectionReadyWorkResults = listOf(
        ConnectionReadyWorkResult.RemovalRequested(Trackable(""), {}, Result.success(Unit)),
    )
    private val removeTrackableWorkResults = listOf(
        RemoveTrackableWorkResult.Success({}, Trackable("")),
        RemoveTrackableWorkResult.Fail({}, Exception()),
        RemoveTrackableWorkResult.NotPresent({}),
    )
    private val retrySubscribeToPresenceWorkResults = listOf(
        RetrySubscribeToPresenceWorkResult.Success(Trackable("")),
        RetrySubscribeToPresenceWorkResult.Failure(Trackable(""), {}),
        RetrySubscribeToPresenceWorkResult.TrackableRemoved,
        RetrySubscribeToPresenceWorkResult.ChannelFailed,
    )

    @Test
    fun `should return AddTrackableResultHandler for each AddTrackableWorkResult`() {
        addTrackableWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult, workerFactory, workerQueue)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return AddTrackableResultHandler",
                handler is AddTrackableResultHandler
            )
        }
    }

    @Test
    fun `should return ConnectionCreatedResultHandler for each ConnectionCreatedWorkResult`() {
        connectionCreatedWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult, workerFactory, workerQueue)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return ConnectionCreatedResultHandler",
                handler is ConnectionCreatedResultHandler
            )
        }
    }

    @Test
    fun `should return ConnectionReadyResultHandler for each ConnectionReadyWorkResult`() {
        connectionReadyWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult, workerFactory, workerQueue)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return ConnectionReadyResultHandler",
                handler is ConnectionReadyResultHandler
            )
        }
    }

    @Test
    fun `should return RemoveTrackableResultHandler for each RemoveTrackableWorkResult`() {
        removeTrackableWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult, workerFactory, workerQueue)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return RemoveTrackableResultHandler",
                handler is RemoveTrackableResultHandler
            )
        }
    }

    @Test
    fun `should return RetrySubscribeToPresenceResultHandler for each RetrySubscribeToPresenceWorkResult`() {
        retrySubscribeToPresenceWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult, workerFactory, workerQueue)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return RetrySubscribeToPresenceResultHandler",
                handler is RetrySubscribeToPresenceResultHandler
            )
        }
    }
}
