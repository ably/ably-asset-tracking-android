package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockConnectSuccess
import com.ably.tracking.test.common.mockConnectFailure
import com.ably.tracking.test.common.mockStartConnectionFailure
import com.ably.tracking.test.common.mockStartConnectionSuccess
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class StartConnectionWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "123123"
    private val subscriberInteractor: SubscriberInteractor = mockk {
        every { updateTrackableState(any()) } just runs
    }
    private val callbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val startConnectionWorker = StartConnectionWorker(ably, subscriberInteractor, trackableId, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call ably connect and post update trackable worker specification on success`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockStartConnectionSuccess()
        ably.mockConnectSuccess(trackableId)

        // when
        val updatedProperties =
            startConnectionWorker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(initialProperties, updatedProperties)
        verify { subscriberInteractor.updateTrackableState(initialProperties) }
        coVerify {
            ably.startConnection()
            ably.connect(
                trackableId, initialProperties.presenceData,
                useRewind = true,
                willSubscribe = true
            )
        }
        Assert.assertEquals(WorkerSpecification.SubscribeForPresenceMessages(callbackFunction), postedWorks[0])
    }

    @Test
    fun `should call ably connect and notify callback on failure`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockStartConnectionSuccess()
        ably.mockConnectFailure(trackableId)

        // when
        val updatedProperties =
            startConnectionWorker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(initialProperties, updatedProperties)
        verify { subscriberInteractor.updateTrackableState(initialProperties) }
        coVerify {
            ably.startConnection()
            ably.connect(
                trackableId, initialProperties.presenceData,
                useRewind = true,
                willSubscribe = true
            )
        }
        verify { callbackFunction.invoke(match { it.isFailure }) }
        Assert.assertTrue(postedWorks.isEmpty())
    }

    @Test
    fun `should notify callback about failure when starting Ably connection fails`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockStartConnectionFailure()
        ably.mockConnectSuccess(trackableId)

        // when
        val updatedProperties =
            startConnectionWorker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(initialProperties, updatedProperties)
        verify { subscriberInteractor.updateTrackableState(initialProperties) }
        coVerify { ably.startConnection() }
        coVerify(exactly = 0) {
            ably.connect(
                trackableId, initialProperties.presenceData,
                useRewind = true,
                willSubscribe = true
            )
        }
        verify { callbackFunction.invoke(match { it.isFailure }) }
        Assert.assertTrue(postedWorks.isEmpty())
    }
}
