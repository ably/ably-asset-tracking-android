package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class SubscribeForPresenceMessagesWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "123123"
    private val callbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val subscribeForPresenceMessagesWorker =
        SubscribeForPresenceMessagesWorker(ably, trackableId, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should post update presence work when presence listener is called`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0))
        val presenceListenerSlot: CapturingSlot<(PresenceMessage) -> Unit> = slot()
        val presenceMessage = createPresenceMessage()
        ably.mockSubscribeToPresenceSuccess(trackableId, presenceListenerSlot)

        // when
        subscribeForPresenceMessagesWorker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()
        presenceListenerSlot.captured.invoke(presenceMessage)

        // then
        Assert.assertEquals(WorkerSpecification.UpdatePublisherPresence(presenceMessage), postedWorks[1])
    }

    private fun createPresenceMessage() = PresenceMessage(
        PresenceAction.PRESENT_OR_ENTER,
        PresenceData(ClientTypes.PUBLISHER, null, null),
        clientId = ""
    )

    @Test
    fun `should post subscribe to channel work when subscribe to presence returns success`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        subscribeForPresenceMessagesWorker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(WorkerSpecification.SubscribeToChannel(callbackFunction), postedWorks[0])
    }

    @Test
    fun `should post disconnect work when subscribe to presence returns failure`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        subscribeForPresenceMessagesWorker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )
        asyncWorks.executeAll()

        // then
        Assert.assertTrue(postedWorks[0] is WorkerSpecification.Disconnect)
    }
}
