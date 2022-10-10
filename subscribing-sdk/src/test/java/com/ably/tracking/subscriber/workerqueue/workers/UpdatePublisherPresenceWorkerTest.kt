package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class UpdatePublisherPresenceWorkerTest {

    private val subscriberInteractor: SubscriberInteractor = mockk {
        every { updatePublisherPresence(any(), any()) } just runs
        every { updateTrackableState(any()) } just runs
        every { updatePublisherResolutionInformation(any()) } just runs
    }

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should call subscriber interactor for PRESENT_OR_ENTER presence message`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        val presenceMessage = createPresenceMessage(PresenceAction.PRESENT_OR_ENTER)
        val worker = UpdatePublisherPresenceWorker(presenceMessage, subscriberInteractor)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberInteractor.updatePublisherPresence(initialProperties, true)
            subscriberInteractor.updateTrackableState(initialProperties)
            subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
        }
    }

    @Test
    fun `should call subscriber interactor for LEAVE_OR_ABSENT presence message`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        val presenceMessage = createPresenceMessage(PresenceAction.LEAVE_OR_ABSENT)
        val worker = UpdatePublisherPresenceWorker(presenceMessage, subscriberInteractor)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberInteractor.updatePublisherPresence(initialProperties, false)
            subscriberInteractor.updateTrackableState(initialProperties)
        }
    }

    @Test
    fun `should call subscriber interactor for UPDATE presence message`() = runBlockingTest {
        // given
        val initialProperties = Properties(Resolution(Accuracy.BALANCED, 100, 100.0))
        val presenceMessage = createPresenceMessage(PresenceAction.UPDATE)
        val worker = UpdatePublisherPresenceWorker(presenceMessage, subscriberInteractor)

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        verify {
            subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
        }
    }

    private fun createPresenceMessage(action: PresenceAction) = PresenceMessage(
        action,
        PresenceData(ClientTypes.PUBLISHER, null, null),
        clientId = ""
    )
}
