package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@ExperimentalCoroutinesApi
class TrackableRemovalSuccessWorkerTest {

    private val trackable = Trackable("test-trackable")
    private val worker = TrackableRemovalSuccessWorker(trackable, Result.success(true))
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should remove trackable from guard`() {
        // given
        val properties = createPublisherProperties()
        properties.trackables.add(trackable)
        val removeTrackableCallbackFunction: ResultCallbackFunction<Boolean> = mockk(relaxed = true)
        properties.trackableRemovalGuard.markForRemoval(trackable, removeTrackableCallbackFunction)

        // when
        worker.doWork(
            properties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        assertThat(asyncWorks).isEmpty()
        assertThat(postedWorks).isEmpty()

        assertThat(properties.trackableRemovalGuard.isMarkedForRemoval(trackable)).isFalse()
        verify(exactly = 1) {
            removeTrackableCallbackFunction.invoke(
                match {
                    it.getOrNull() == true
                }
            )
        }
    }
}
