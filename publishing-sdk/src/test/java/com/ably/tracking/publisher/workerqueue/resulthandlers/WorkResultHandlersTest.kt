package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Test

// Without it we're not allowed to check result handler type with `is`
@Suppress("INCOMPATIBLE_TYPES")
class WorkResultHandlersTest {
    private val addTrackableWorkResults = listOf(
        AddTrackableWorkResult.AlreadyIn(MutableStateFlow(TrackableState.Online), {}),
        AddTrackableWorkResult.Success(Trackable(""), {}),
        AddTrackableWorkResult.Fail(Trackable(""), null, {}),
    )
    private val connectionCreatedWorkResults = listOf(
        ConnectionCreatedWorkResult.RemovalRequested(Trackable(""), {}, Result.success(Unit)),
        ConnectionCreatedWorkResult.PresenceSuccess(Trackable(""), {}, {}),
        ConnectionCreatedWorkResult.PresenceFail(Trackable(""), {}, ConnectionException(ErrorInformation(""))),
    )
    private val connectionReadyWorkResults = listOf(
        ConnectionReadyWorkResult.RemovalRequested(Trackable(""), {}, Result.success(Unit)),
    )

    @Test
    fun `should return AddTrackableResultHandler for each AddTrackableWorkResult`() {
        addTrackableWorkResults.forEach {
            // given
            val workResult = it

            // when
            val handler = getWorkResultHandler(workResult)

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
            val handler = getWorkResultHandler(workResult)

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
            val handler = getWorkResultHandler(workResult)

            // then
            Assert.assertTrue(
                "Work result ${it::class.simpleName} should return ConnectionReadyResultHandler",
                handler is ConnectionReadyResultHandler
            )
        }
    }
}
