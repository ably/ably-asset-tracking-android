package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.PresenceData
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockEnterPresenceFailure
import com.ably.tracking.test.common.mockEnterPresenceSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnterPresenceWorkerTest {

    private val trackableId = "testtrackable"

    private val ably: Ably = mockk {
        coEvery { startConnection() } returns Result.success(Unit)
    }

    private val worker = EnterPresenceWorker(trackableId, ably)

    private val initialProperties: SubscriberProperties = mockk {
        every { presenceData } returns PresenceData(ClientTypes.SUBSCRIBER, null, null)
    }

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post no work when connection succeeded`() {
        runTest {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceSuccess(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            assertThat(postedWorks).isEmpty()
        }
    }

    @Test
    fun `should post EnterPresence work after delay when enter presence failed with a non-fatal error`() {
        runTest(context = UnconfinedTestDispatcher()) {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceFailure(trackableId, isFatal = false)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.launchAll(this)

            assertThat(postedWorks).isEmpty()

            advanceUntilIdle()

            assertThat(postedWorks)
                .contains(WorkerSpecification.EnterPresence(trackableId))
        }
    }

    @Test
    fun `should post Disconnect work when connection failed with a fatal error`() {
        runTest {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceFailure(trackableId, isFatal = true)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWork = postedWorks[0] as WorkerSpecification.Disconnect
            assertThat(postedWork.trackableId).isEqualTo(trackableId)
        }
    }

    @Test
    fun `should post EnterPresence work when connection failed with a fatal error with 91001 error code`() {
        runTest {
            // given
            mockChannelStateChange(ConnectionState.ONLINE)
            ably.mockEnterPresenceChannelSuspendedException(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.launchAll(this)

            assertThat(postedWorks).isEmpty()

            advanceUntilIdle()

            assertThat(postedWorks)
                .contains(WorkerSpecification.EnterPresence(trackableId))
        }
    }

    private fun Ably.mockEnterPresenceChannelSuspendedException(trackableId: String) {
        coEvery {
            enterChannelPresence(trackableId, any())
        } returns Result.failure(channelSuspendedException())
    }

    // returns connection exception specific to enter presence on a suspended channel
    private fun channelSuspendedException() = ConnectionException(
        ErrorInformation(
            code = 91001,
            statusCode = 400,
            message = "Test",
            href = null,
            cause = null
        )
    )

    @Test
    fun `should post FailTrackable work when connection when channel transitions to failed`() {
        runTest {
            // given
            mockChannelStateChange(ConnectionState.FAILED)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWork = postedWorks[0] as WorkerSpecification.Disconnect
            assertThat(postedWork.trackableId).isEqualTo(trackableId)
        }
    }

    private fun mockChannelStateChange(newState: ConnectionState) {
        every {
            runBlocking {
                ably.waitForChannelToAttach(trackableId)
            }
        } returns when (newState) {
            ConnectionState.ONLINE -> Result.success(Unit)
            ConnectionState.FAILED -> Result.failure(ConnectionException(ErrorInformation("Channel attach failed")))
            ConnectionState.OFFLINE -> throw Exception("Not a valid result")
        }
    }
}
