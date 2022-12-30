package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberStoppedException
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockCloseFailure
import com.ably.tracking.test.common.mockCloseSuccess
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
internal class StopConnectionWorkerTest {

    private val ably: Ably = mockk()
    private val subscriberInteractor: SubscriberInteractor = mockk {
        every { notifyAssetIsOffline() } just runs
    }
    private val callbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val stopConnectionWorker = StopConnectionWorker(ably, subscriberInteractor, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call ably close and notify callback with success`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        ably.mockCloseSuccess()

        // when
        val updatedProperties =
            stopConnectionWorker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // then
        Assert.assertTrue(updatedProperties.isStopped)
        verify { callbackFunction.invoke(match { it.isSuccess }) }
        verify { subscriberInteractor.notifyAssetIsOffline() }
    }

    @Test
    fun `should call ably close and notify callback with failure when it fails`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        ably.mockCloseFailure()

        // when
        stopConnectionWorker.doWork(initialProperties, asyncWorks.appendWork(), postedWorks.appendSpecification())

        // then
        verify { callbackFunction.invoke(match { it.isFailure }) }
    }

    @Test
    fun `should call the callback function with a success if subscriber is already stopped`() = runBlockingTest {
        // given
        ably.mockCloseSuccess()

        // when
        stopConnectionWorker.doWhenStopped(SubscriberStoppedException())

        // then
        verify { callbackFunction.invoke(match { it.isSuccess }) }
    }
}
