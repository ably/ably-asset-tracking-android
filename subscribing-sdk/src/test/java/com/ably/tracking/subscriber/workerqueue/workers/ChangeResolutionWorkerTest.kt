package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.test.common.mockUpdatePresenceDataSuccess
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class ChangeResolutionWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "123123"
    private val updatedResolution = Resolution(Accuracy.HIGH, 10, 10.0)
    private val callbackFunction: ResultCallbackFunction<Unit> = mockk(relaxed = true)
    private val changeResolutionWorker = ChangeResolutionWorker(ably, trackableId, updatedResolution, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()

    @Test
    fun `should return Properties with updated resolution and notify callback`() = runBlockingTest {
        // given
        val initialProperties = SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
        ably.mockUpdatePresenceDataSuccess(trackableId)

        // when
        val updatedProperties = changeResolutionWorker.doWork(initialProperties, asyncWorks.appendWork()) {}
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(updatedResolution, updatedProperties.presenceData.resolution)
        verify { callbackFunction.invoke(match { it.isSuccess }) }
    }
}
