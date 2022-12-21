package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class ProcessInitialPresenceMessagesWorkerTest {

    private val subscriberProperties: SubscriberProperties = mockk()
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should process all presence messages`() = runBlockingTest {
        // given
        val presenceMessages = listOf(anyPresenceMessage(), anyPresenceMessage(), anyPresenceMessage())
        val worker = ProcessInitialPresenceMessagesWorker(presenceMessages) {}
        every { subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(any()) } just Runs

        // when
        worker.doWork(
            subscriberProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification(),
        )

        // then
        verify {
            subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(presenceMessages)
        }
    }

    @Test
    fun `should post subscribe to channel work after processing presence messages`() = runBlockingTest {
        // given
        val callbackFunction: ResultCallbackFunction<Unit> = {}
        val presenceMessages = emptyList<PresenceMessage>()
        val worker = ProcessInitialPresenceMessagesWorker(presenceMessages, callbackFunction)
        every { subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(any()) } just Runs

        // when
        worker.doWork(
            subscriberProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification(),
        )

        // then
        Assert.assertEquals(WorkerSpecification.SubscribeToChannel(callbackFunction), postedWorks[0])
        verify {
            subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(presenceMessages)
        }
    }

    private fun anyPresenceMessage() = PresenceMessage(
        action = PresenceAction.PRESENT_OR_ENTER,
        data = PresenceData(ClientTypes.PUBLISHER, null, null),
        memberKey = "",
    )
}
