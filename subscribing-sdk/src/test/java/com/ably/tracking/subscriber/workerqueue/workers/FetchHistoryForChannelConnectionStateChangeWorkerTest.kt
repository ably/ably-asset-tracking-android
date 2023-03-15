package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class FetchHistoryForChannelConnectionStateChangeWorkerTest {

    private val trackableId = "trackableId"
    private val subscriberProperties: SubscriberProperties = mockk()
    private val channelConnectionStateChange: ConnectionStateChange = mockk()
    private val ably: Ably = mockk()
    private val fetchHistoryForChannelConnectionStateChangeWorker =
        FetchHistoryForChannelConnectionStateChangeWorker(
            trackableId,
            channelConnectionStateChange,
            ably
        )

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `when new state is not ONLINE should post no async work and post update worker`() =
        runTest {
            // given
            every { channelConnectionStateChange.state } returns ConnectionState.OFFLINE

            // when
            fetchHistoryForChannelConnectionStateChangeWorker.doWork(
                subscriberProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            assertThat(asyncWorks).isEmpty()

            val postedWork = postedWorks[0] as WorkerSpecification.UpdateChannelConnectionState
            assertThat(postedWork.channelConnectionStateChange).isEqualTo(
                channelConnectionStateChange
            )
            assertThat(postedWork.presenceHistory).isNull()
        }

    @Test
    fun `when new state is ONLINE should post async work and post update history worker`() =
        runTest {
            // given
            every { channelConnectionStateChange.state } returns ConnectionState.ONLINE

            val presenceHistory = listOf(
                PresenceMessage(
                    PresenceAction.UPDATE,
                    PresenceData(ClientTypes.PUBLISHER),
                    ""
                )
            )
            coEvery { ably.getRecentPresenceHistory(trackableId, any()) } returns Result.success(
                presenceHistory
            )

            // when
            fetchHistoryForChannelConnectionStateChangeWorker.doWork(
                subscriberProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            asyncWorks.executeAll()

            // then
            val postedWork = postedWorks[0] as WorkerSpecification.UpdateChannelConnectionState
            assertThat(postedWork.channelConnectionStateChange).isEqualTo(
                channelConnectionStateChange
            )
            assertThat(postedWork.presenceHistory).isEqualTo(presenceHistory)
        }

    @Test
    fun `when fetching history fails should post update history worker without history`() =
        runTest {
            // given
            every { channelConnectionStateChange.state } returns ConnectionState.ONLINE
            coEvery { ably.getRecentPresenceHistory(trackableId, any()) } returns Result.failure(
                RuntimeException()
            )

            // when
            fetchHistoryForChannelConnectionStateChangeWorker.doWork(
                subscriberProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            asyncWorks.executeAll()

            // then
            val postedWork = postedWorks[0] as WorkerSpecification.UpdateChannelConnectionState
            assertThat(postedWork.channelConnectionStateChange).isEqualTo(
                channelConnectionStateChange
            )
            assertThat(postedWork.presenceHistory).isNull()
        }
}
