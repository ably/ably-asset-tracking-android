package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import com.ably.tracking.test.common.mockUpdatePresenceDataFailure
import com.ably.tracking.test.common.mockUpdatePresenceDataSuccess
import com.ably.tracking.test.common.mockWaitForChannelToAttachFailure
import com.ably.tracking.test.common.mockWaitForChannelToAttachSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateResolutionWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "testtrackable"

    private val resolution: Resolution = Resolution(Accuracy.BALANCED, 100L, 100.0)

    private val worker = UpdateResolutionWorker(trackableId, resolution, ably)

    private val initialProperties = createPublisherProperties()
    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call updatePresenceData when waiting for channel returns success`() {
        runTest {
            // given
            ably.mockWaitForChannelToAttachSuccess(trackableId)
            ably.mockUpdatePresenceDataSuccess(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            coVerify {
                ably.updatePresenceData(trackableId, match { it.resolution == resolution })
            }
        }
    }

    @Test
    fun `should not call updatePresenceData and post work when waiting for channel returns failure`() {
        runTest {
            // given
            ably.mockWaitForChannelToAttachFailure(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            coVerify(exactly = 0) {
                ably.updatePresenceData(trackableId, any())
            }
            val postedWorker = postedWorks[0] as WorkerSpecification.UpdateResolution
            assertThat(postedWorker.resolution).isEqualTo(resolution)
            assertThat(postedWorker.trackableId).isEqualTo(trackableId)
        }
    }

    @Test
    fun `should not throw any exception when updatePresenceData returns failure`() {
        runTest {
            // given
            ably.mockWaitForChannelToAttachSuccess(trackableId)
            ably.mockUpdatePresenceDataFailure(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            try {
                asyncWorks.executeAll()
            } catch (exception: java.lang.Exception) {
                Assert.fail("Should not throw any exceptions")
            }
        }
    }

    @Test
    fun `should post  when updatePresenceData returns failure`() {
        runTest {
            // given
            ably.mockWaitForChannelToAttachSuccess(trackableId)
            ably.mockUpdatePresenceDataFailure(trackableId)

            // when
            worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()

            val postedWorker = postedWorks[0] as WorkerSpecification.UpdateResolution
            assertThat(postedWorker.resolution).isEqualTo(resolution)
            assertThat(postedWorker.trackableId).isEqualTo(trackableId)
        }
    }
}
