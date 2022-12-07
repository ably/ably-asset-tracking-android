package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.TimeProvider
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DestinationSetWorkerTest {
    private val timeProvider: TimeProvider = mockk()
    private val routeDurationInMilliseconds = 500L
    private val worker = DestinationSetWorker(
        routeDurationInMilliseconds = routeDurationInMilliseconds,
        timeProvider = timeProvider
    )
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should calculate the eta by adding route duration to the current time`() {
        // given
        val initialProperties = createPublisherProperties()
        timeProvider.mockCurrentTime(1000L)

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(updatedProperties.estimatedArrivalTimeInMilliseconds)
            .isEqualTo(1500L)
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
    }

    private fun TimeProvider.mockCurrentTime(currentTimeInMilliseconds: Long) {
        every { getCurrentTimeInMilliseconds() } returns currentTimeInMilliseconds
    }
}
