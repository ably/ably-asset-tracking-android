package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.publisher.CorePublisher
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
    private val publisher: CorePublisher = mockk {
        every { resolveResolution(any(), any()) } just runs
    }

    private val worker = RefreshResolutionPolicyWorker(publisher)

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
            publisher.resolveResolution(firstTrackable, initialProperties)
            publisher.resolveResolution(secondTrackable, initialProperties)
            publisher.resolveResolution(thirdTrackable, initialProperties)
        }
    }
}
