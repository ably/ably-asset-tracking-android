package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class UpdatePublisherPresenceWorkerTest {

    private val subscriberProperties: SubscriberProperties = mockk()
    private val presenceMessage: PresenceMessage = mockk()
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call updateForPresenceMessagesAndThenEmitStateEventsIfRequired`() = runBlockingTest {
        // given
        val worker = UpdatePublisherPresenceWorker(presenceMessage)
        every { subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(any()) } just Runs

        // when
        worker.doWork(
            subscriberProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberProperties.updateForPresenceMessagesAndThenEmitStateEventsIfRequired(listOf(presenceMessage))
        }
    }
}
