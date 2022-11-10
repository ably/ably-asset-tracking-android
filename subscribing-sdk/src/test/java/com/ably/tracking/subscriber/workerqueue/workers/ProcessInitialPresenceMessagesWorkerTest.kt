package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.processPresenceMessage
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
internal class ProcessInitialPresenceMessagesWorkerTest {

    private val subscriberInteractor: SubscriberInteractor = mockk()
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Before
    fun setup() {
        mockkStatic("com.ably.tracking.subscriber.PresenceMessageProcessorKt")
        every { processPresenceMessage(any(), any(), any()) } just runs
    }

    @After
    fun cleanup() {
        unmockkStatic("com.ably.tracking.subscriber.PresenceMessageProcessorKt")
    }

    @Test
    fun `should process all presence messages`() = runBlockingTest {
        // given
        val initialProperties = anyProperties()
        val presenceMessages = listOf(anyPresenceMessage(), anyPresenceMessage(), anyPresenceMessage())
        val worker = ProcessInitialPresenceMessagesWorker(presenceMessages, subscriberInteractor) {}

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify(exactly = presenceMessages.size) {
            processPresenceMessage(any(), initialProperties, subscriberInteractor)
        }
    }

    @Test
    fun `should post subscribe to channel work after processing presence messages`() = runBlockingTest {
        // given
        val callbackFunction: ResultCallbackFunction<Unit> = {}
        val worker = ProcessInitialPresenceMessagesWorker(emptyList(), subscriberInteractor, callbackFunction)

        // when
        worker.doWork(
            anyProperties(),
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        Assert.assertEquals(WorkerSpecification.SubscribeToChannel(callbackFunction), postedWorks[0])
    }

    private fun anyPresenceMessage() = PresenceMessage(
        PresenceAction.PRESENT_OR_ENTER,
        PresenceData(ClientTypes.PUBLISHER, null, null),
        clientId = ""
    )

    private fun anyProperties() = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0))
}
