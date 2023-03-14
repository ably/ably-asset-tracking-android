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
import com.ably.tracking.test.common.mockGetCurrentPresenceError
import com.ably.tracking.test.common.mockGetCurrentPresenceSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    fun `should post update presence work when presence listener is called`() = runTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        val presenceListenerSlot: CapturingSlot<(PresenceMessage) -> Unit> = slot()
        val presenceMessage = createPresenceMessage()
        ably.mockGetCurrentPresenceSuccess(trackableId)
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
        action = PresenceAction.PRESENT_OR_ENTER,
        data = PresenceData(ClientTypes.PUBLISHER, null, null),
        timestamp = 123,
        memberKey = "",
    )

    @Test
    fun `should post process initial presence messages work when both get current presence and subscribe to presence return success`() =
        runTest {
            // given
            val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
            val initialPresenceMessages = listOf(anyPresenceMessage())
            ably.mockGetCurrentPresenceSuccess(trackableId, initialPresenceMessages)
            ably.mockSubscribeToPresenceSuccess(trackableId)

            // when
            subscribeForPresenceMessagesWorker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
            asyncWorks.executeAll()

            // then
            Assert.assertEquals(
                WorkerSpecification.ProcessInitialPresenceMessages(
                    initialPresenceMessages,
                    callbackFunction
                ),
                postedWorks[0],
            )
        }

    @Test
    fun `should post disconnect work when subscribe to presence returns failure`() = runTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        ably.mockGetCurrentPresenceSuccess(trackableId)
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

    @Test
    fun `should post disconnect work when get current presence returns failure`() = runTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        ably.mockGetCurrentPresenceError(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

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

    @Test
    fun `should post disconnect work when async work throw an unexpected exception`() = runTest {
        // given

        // when
        subscribeForPresenceMessagesWorker.onUnexpectedAsyncError(
            Exception("Unexpected exception"),
            postedWorks.appendSpecification(),
        )

        // then
        Assert.assertTrue(postedWorks[0] is WorkerSpecification.Disconnect)
    }

    private fun anyPresenceMessage() =
        PresenceMessage(PresenceAction.PRESENT_OR_ENTER, PresenceData(ClientTypes.PUBLISHER), 123,"any-client-id")
}
