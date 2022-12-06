package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class ChannelConnectionStateChangeWorkerTest {
    private val trackableId = "123123"
    private val connectionStateChange = ConnectionStateChange(ConnectionState.ONLINE, null)
    private val publisher: CorePublisher = mockk {
        every { updateTrackableState(any(), trackableId) } just runs
    }
    private val worker = ChannelConnectionStateChangeWorker(
        trackableId = trackableId,
        connectionStateChange = connectionStateChange,
        publisher = publisher,
        logHandler = null
    )

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should update channel connection state and notify publisher`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(updatedProperties.lastChannelConnectionStateChanges[trackableId])
            .isEqualTo(connectionStateChange)
        assertThat(asyncWorks)
            .isEmpty()
        assertThat(postedWorks)
            .isEmpty()

        verify {
            publisher.updateTrackableState(updatedProperties, trackableId)
        }
    }
}
