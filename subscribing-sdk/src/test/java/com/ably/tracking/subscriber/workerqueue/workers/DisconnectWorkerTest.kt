package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.test.common.mockDisconnectSuccess
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
internal class DisconnectWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "123123"
    private val callbackFunction: () -> Unit = mockk(relaxed = true)
    private val disconnectWorker = DisconnectWorker(ably, trackableId, callbackFunction)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()

    @Test
    fun `should call ably disconnect and notify callback`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        ably.mockDisconnectSuccess(trackableId)

        // when
        val updatedProperties = disconnectWorker.doWork(initialProperties, asyncWorks.appendWork()) {}
        asyncWorks.executeAll()

        // then
        Assert.assertEquals(initialProperties, updatedProperties)
        coVerify { ably.disconnect(trackableId, initialProperties.presenceData) }
        verify { callbackFunction.invoke() }
    }
}
