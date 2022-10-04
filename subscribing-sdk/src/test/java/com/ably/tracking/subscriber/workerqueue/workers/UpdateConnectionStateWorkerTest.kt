package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
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
internal class UpdateConnectionStateWorkerTest {

    private val subscriberInteractor: SubscriberInteractor = mockk {
        every { updateTrackableState(any()) } just runs
    }
    private val channelConnectionStateChange = ConnectionStateChange(ConnectionState.ONLINE, null)
    private val updateConnectionStateWorker =
        UpdateConnectionStateWorker(channelConnectionStateChange, subscriberInteractor)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call updateTrackableState and update properties`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))

        // when
        val updatedProperties =
            updateConnectionStateWorker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

        // then
        Assert.assertEquals(channelConnectionStateChange, updatedProperties.lastConnectionStateChange)
        verify { subscriberInteractor.updateTrackableState(updatedProperties) }
    }
}
