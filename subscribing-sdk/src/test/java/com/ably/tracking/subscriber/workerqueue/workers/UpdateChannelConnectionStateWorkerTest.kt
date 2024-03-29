package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class UpdateChannelConnectionStateWorkerTest {

    private val subscriberProperties: SubscriberProperties = mockk()
    private val channelConnectionStateChange: ConnectionStateChange = mockk()
    private val presenceHistory: List<PresenceMessage> = emptyList()
    private val updateChannelConnectionStateWorker =
        UpdateChannelConnectionStateWorker(channelConnectionStateChange, presenceHistory)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired`() = runTest {
        // given
        every { subscriberProperties.updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired(any(), any()) } just Runs

        // when
        updateChannelConnectionStateWorker.doWork(
            subscriberProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberProperties.updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired(
                channelConnectionStateChange, presenceHistory
            )
        }
    }
}
