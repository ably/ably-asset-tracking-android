package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class ChangeLocationEngineResolutionWorkerTest {
    private val resolutionPolicy = mockk<ResolutionPolicy>()
    private val mapbox = mockk<Mapbox>{
        every { changeResolution(any()) } just runs
    }

    private val worker = ChangeLocationEngineResolutionWorker(resolutionPolicy, mapbox)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should update channel connection state and notify publisher`() {
        // given
        val allResolutions = setOf(
            Resolution(Accuracy.BALANCED, 1L, 1.0),
            Resolution(Accuracy.BALANCED, 2L, 2.0),
        )
        val initialProperties = createPublisherProperties()
            .insertResolutions(allResolutions)
        val newlyCalculatedResolution = Resolution(Accuracy.HIGH, 123L, 123.0)

        every { resolutionPolicy.resolve(allResolutions) } returns newlyCalculatedResolution

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(updatedProperties.locationEngineResolution)
            .isEqualTo(newlyCalculatedResolution)
        assertThat(asyncWorks)
            .isEmpty()
        assertThat(postedWorks)
            .isEmpty()

        verify {
            mapbox.changeResolution(newlyCalculatedResolution)
        }
    }

    @Test
    fun `should do nothing if publisher is using constant location engine resolution`() {
        // given
        val initialResolution = Resolution(Accuracy.HIGH, 1L, 1.0)
        val initialProperties = createPublisherProperties(
            isLocationEngineResolutionConstant = true,
            locationEngineResolution = initialResolution
        )

        // when
        val updatedProperties = worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(updatedProperties.locationEngineResolution).isEqualTo(initialResolution)
        verify(exactly = 0) {
            mapbox.changeResolution(any())
        }
    }
}
