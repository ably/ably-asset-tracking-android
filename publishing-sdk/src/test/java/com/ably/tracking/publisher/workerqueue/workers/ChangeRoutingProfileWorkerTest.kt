package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class ChangeRoutingProfileWorkerTest {
    private val publisher: CorePublisher = mockk {
        every { setDestination(any(), any()) } just runs
    }
    private val routingProfile = RoutingProfile.WALKING
    private val worker = ChangeRoutingProfileWorker(
        routingProfile = routingProfile,
        corePublisher = publisher
    )

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()


    @Test
    fun `should set the routing profile`() {
        // given
        val initialProperties = createPublisherProperties()

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        assertThat(updatedProperties.routingProfile)
            .isEqualTo(routingProfile)
    }

    @Test
    fun `should refresh the destination with the new routing profile if the current destination is present`() {
        // given
        val initialProperties = createPublisherProperties()
        val currentDestination = Destination(1.0, 1.0)
        initialProperties.currentDestination = currentDestination

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        verify(exactly = 1) {
            publisher.setDestination(currentDestination, updatedProperties)
        }
    }

    @Test
    fun `should not refresh the destination with the new routing profile if there is no current destination`() {
        // given
        val initialProperties = createPublisherProperties()
        initialProperties.currentDestination = null

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()
        verify(exactly = 0) {
            publisher.setDestination(any(), any())
        }
    }
}
