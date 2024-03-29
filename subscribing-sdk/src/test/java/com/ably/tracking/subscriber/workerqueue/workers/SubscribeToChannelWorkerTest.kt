package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class SubscribeToChannelWorkerTest {

    private val subscriberInteractor: SubscriberInteractor = mockk {
        every { subscribeForChannelState() } just runs
        every { subscribeForEnhancedEvents(any()) } just runs
        every { subscribeForRawEvents(any()) } just runs
    }

    private val subscribeToChannelWorker = SubscribeToChannelWorker(subscriberInteractor)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should notify callback after calling subscriberInteractor`() = runTest {
        // given
        val initialProperties =
            SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())

        // when
        subscribeToChannelWorker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberInteractor.subscribeForChannelState()
            subscriberInteractor.subscribeForEnhancedEvents(initialProperties.presenceData)
            subscriberInteractor.subscribeForRawEvents(initialProperties.presenceData)
        }
    }
}
