package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class AblyConnectionStateChangeWorkerTest {
    private val connectionStateChange = ConnectionStateChange(ConnectionState.ONLINE, null)
    private val publisher: CorePublisher = mockk {
        every { updateTrackableState(any(), any()) } just runs

    }
    private val worker = AblyConnectionStateChangeWorker(
        connectionStateChange = connectionStateChange,
        publisher = publisher,
        logHandler = null
    )

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should update trackable state for all trackables and notify publisher`() {
        // given
        val trackables = listOf(Trackable("first"), Trackable("second"), Trackable("third"))
        val initialProperties = createPublisherProperties()
        trackables.forEach {
            initialProperties.trackables.add(it)
        }

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(updatedProperties.lastConnectionStateChange).isEqualTo(connectionStateChange)
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            trackables.forEach {
                publisher.updateTrackableState(updatedProperties, it.id)
            }
        }
    }
}
