package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
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
class ChangeResolutionWorkerTest {

    private val ably: Ably = mockk()
    private val trackableId = "testtrackable"

    private val resolution: Resolution = mockk()

    private val worker = ChangeResolutionWorker(ably, trackableId, resolution)

    private val initialProperties = createSubscriberProperties()
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
                ably.updatePresenceData(trackableId, initialProperties.presenceData)
            }
        }
    }

    @Test
    fun `should not call updatePresenceData, not post work and remove resolution from properties when newer resolution is present`() =
        runTest {
            // given
            initialProperties.addUpdatingResolution(trackableId, resolution)
            initialProperties.addUpdatingResolution(
                trackableId,
                Resolution(Accuracy.HIGH, 50L, 50.0)
            )
            ably.mockWaitForChannelToAttachFailure(trackableId)

            // when
            val updatedProperties = worker.doWork(
                initialProperties,
                asyncWorks.appendWork(),
                postedWorks.appendSpecification()
            )

            // then
            asyncWorks.executeAll()
            assertThat(postedWorks).hasSize(0)
            assertThat(
                updatedProperties.containsUpdatingResolution(
                    trackableId,
                    resolution
                )
            ).isFalse()
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
            val postedWorker = postedWorks[0] as WorkerSpecification.ChangeResolution
            assertThat(postedWorker.resolution).isEqualTo(resolution)
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

            val postedWorker = postedWorks[0] as WorkerSpecification.ChangeResolution
            assertThat(postedWorker.resolution).isEqualTo(resolution)
        }
    }
}
