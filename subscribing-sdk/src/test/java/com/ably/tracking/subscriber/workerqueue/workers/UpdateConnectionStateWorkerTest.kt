package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
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
internal class UpdateConnectionStateWorkerTest {

    private val subscriberProperties: SubscriberProperties = mockk()
    private val connectionStateChange: ConnectionStateChange = mockk()
    private val updateConnectionStateWorker =
        UpdateConnectionStateWorker(connectionStateChange)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call updateForConnectionStateChangeAndThenEmitStateEventsIfRequired`() = runBlockingTest {
        // given
        every { subscriberProperties.updateForConnectionStateChangeAndThenEmitStateEventsIfRequired(any()) } just Runs

        // when
        updateConnectionStateWorker.doWork(
            subscriberProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberProperties.updateForConnectionStateChangeAndThenEmitStateEventsIfRequired(
                connectionStateChange
            )
        }
    }
}
