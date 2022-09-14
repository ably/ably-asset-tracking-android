package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ChangeLocationEngineResolutionWorkerTest {
    private lateinit var worker: ChangeLocationEngineResolutionWorker
    private val resolutionPolicy = mockk<ResolutionPolicy>(relaxed = true)
    private val mapbox = mockk<Mapbox>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = ChangeLocationEngineResolutionWorker(resolutionPolicy, mapbox)
    }

    @After
    fun cleanUp() {
        clearAllMocks()
    }

    @Test
    fun `should always return an empty result`() {
        // given

        // when
        val result = worker.doWork(publisherProperties)

        // then
        Assert.assertNull(result.syncWorkResult)
        Assert.assertNull(result.asyncWork)
    }

    @Test
    fun `should resolve new resolution using all resolutions for all trackables`() {
        // given
        val allResolutions = setOf(
            Resolution(Accuracy.BALANCED, 1L, 1.0),
            Resolution(Accuracy.BALANCED, 2L, 2.0),
        )
        mockResolutions(allResolutions)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            resolutionPolicy.resolve(allResolutions)
        }
    }

    @Test
    fun `should set the newly calculated resolution as the location engine resolution`() {
        // given
        val newlyCalculatedResolution = Resolution(Accuracy.HIGH, 123L, 123.0)
        mockCalculatingResolution(newlyCalculatedResolution)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            publisherProperties.locationEngineResolution = newlyCalculatedResolution
        }
    }

    @Test
    fun `should change the location engine resolution to the newly calculated one`() {
        // given
        val newlyCalculatedResolution = Resolution(Accuracy.HIGH, 123L, 123.0)
        mockCalculatingResolution(newlyCalculatedResolution)

        // when
        worker.doWork(publisherProperties)

        // then
        coVerify(exactly = 1) {
            mapbox.changeResolution(newlyCalculatedResolution)
        }
    }

    @Test
    fun `should do nothing if publisher is using constant location engine resolution`() {
        // given
        mockIsUsingConstantLocationEngineResolution()

        // when
        worker.doWork(publisherProperties)

        // then
        coVerify(exactly = 0) {
            resolutionPolicy.resolve(any<Set<Resolution>>())
            publisherProperties.locationEngineResolution = any()
            mapbox.changeResolution(any())
        }
    }

    private fun mockResolutions(resolutionSet: Set<Resolution>) {
        val resolutions = resolutionSet
            .mapIndexed { index, resolution -> index.toString() to resolution }
            .toMap()
            .toMutableMap()
        every { publisherProperties.resolutions } returns resolutions
    }

    private fun mockCalculatingResolution(resolution: Resolution) {
        every { resolutionPolicy.resolve(any() as Set<Resolution>) } returns resolution
    }

    private fun mockIsUsingConstantLocationEngineResolution() {
        every { publisherProperties.isLocationEngineResolutionConstant } returns true
    }
}
