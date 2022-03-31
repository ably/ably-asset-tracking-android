package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RefreshResolutionPolicyWorkerTest {
    private lateinit var worker: RefreshResolutionPolicyWorker
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        worker = RefreshResolutionPolicyWorker(corePublisher)
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
    fun `should resolve resolution for all trackables`() {
        // given
        val firstTrackable = Trackable("first")
        val secondTrackable = Trackable("second")
        val thirdTrackable = Trackable("third")
        val allTrackables = mutableSetOf(firstTrackable, secondTrackable, thirdTrackable)
        every { publisherProperties.trackables } returns allTrackables

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.resolveResolution(firstTrackable, publisherProperties)
            corePublisher.resolveResolution(secondTrackable, publisherProperties)
            corePublisher.resolveResolution(thirdTrackable, publisherProperties)
        }
    }
}
