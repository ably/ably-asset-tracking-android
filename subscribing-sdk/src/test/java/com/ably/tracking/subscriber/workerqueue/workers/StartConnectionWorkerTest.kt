package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockConnectSuccess
import com.ably.tracking.test.common.mockEnterPresenceSuccess
import com.ably.tracking.test.common.mockStartConnectionSuccess
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class StartConnectionWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "123123"
    private val callbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val subscriberProperties: SubscriberProperties = mockk()
    private val startConnectionWorker = StartConnectionWorker(ably, trackableId, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call ably connect and post enterPresence worker specification and post update trackable worker specification on success`() = runTest {
        // given
        ably.mockStartConnectionSuccess()
        ably.mockConnectSuccess(trackableId)
        ably.mockEnterPresenceSuccess(trackableId)
        every { subscriberProperties.emitStateEventsIfRequired() } just Runs
        val presenceData: PresenceData = mockk()
        every { subscriberProperties.presenceData } returns presenceData

        // when
        val updatedProperties =
            startConnectionWorker.doWork(
                subscriberProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(subscriberProperties, updatedProperties)
        coVerify {
            ably.startConnection()
            ably.connect(
                trackableId,
                useRewind = true,
                willSubscribe = true
            )
        }
        Assert.assertEquals(WorkerSpecification.SubscribeForPresenceMessages, postedWorks[0])
        Assert.assertEquals(WorkerSpecification.EnterPresence(trackableId), postedWorks[1])
    }
}
