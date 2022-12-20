package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class RefreshResolutionPolicyWorkerTest {
    private val publisherInteractor: PublisherInteractor = mockk {
        every { resolveResolution(any(), any()) } just runs
    }

    private val worker = RefreshResolutionPolicyWorker(publisherInteractor)

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should resolve resolution for all trackables`() {
        // given
        val initialProperties = createPublisherProperties()
        val firstTrackable = Trackable("first")
        initialProperties.trackables.add(firstTrackable)
        val secondTrackable = Trackable("second")
        initialProperties.trackables.add(secondTrackable)
        val thirdTrackable = Trackable("third")
        initialProperties.trackables.add(thirdTrackable)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        Truth.assertThat(asyncWorks).isEmpty()
        Truth.assertThat(postedWorks).isEmpty()

        verify(exactly = 1) {
            publisherInteractor.resolveResolution(firstTrackable, initialProperties)
            publisherInteractor.resolveResolution(secondTrackable, initialProperties)
            publisherInteractor.resolveResolution(thirdTrackable, initialProperties)
        }
    }
}
