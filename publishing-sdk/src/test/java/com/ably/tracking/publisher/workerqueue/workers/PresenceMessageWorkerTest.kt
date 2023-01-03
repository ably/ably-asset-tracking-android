package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
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

class PresenceMessageWorkerTest {
    private val trackable = Trackable("test-trackable")
    private val publisherInteractor: PublisherInteractor = mockk {
        every { updateSubscriber(any(), any(), any(), any()) } just runs
        every { addSubscriber(any(), any(), any(), any()) } just runs
        every { removeSubscriber(any(), any(), any()) } just runs
    }

    private lateinit var worker: PresenceMessageWorker

    private val asyncWorks = mutableListOf<suspend () -> Unit>()
    private val postedWorks = mutableListOf<WorkerSpecification>()

    @Test
    fun `should add subscriber when presence action is enter and type is subscriber`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.PRESENT_OR_ENTER, isSubscriber = true)
        val initialProperties = createPublisherProperties()

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
            publisherInteractor.addSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should not add subscriber when presence action is enter but type is publisher`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.PRESENT_OR_ENTER, isSubscriber = false)
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        Truth.assertThat(asyncWorks).isEmpty()
        Truth.assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.addSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should remove subscriber when presence action is leave and type is subscriber`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.LEAVE_OR_ABSENT, isSubscriber = true)
        val initialProperties = createPublisherProperties()

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
            publisherInteractor.removeSubscriber(any(), trackable, any())
        }
    }

    @Test
    fun `should not remove subscriber when presence action is leave but type is publisher`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.LEAVE_OR_ABSENT, isSubscriber = false)
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        Truth.assertThat(asyncWorks).isEmpty()
        Truth.assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.removeSubscriber(any(), trackable, any())
        }
    }

    @Test
    fun `should update subscriber when presence action is update and type is subscriber`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.UPDATE, isSubscriber = true)
        val initialProperties = createPublisherProperties()

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
            publisherInteractor.updateSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should not update subscriber when presence action is update but type is publisher`() {
        // given
        worker = prepareWorkerWithPresenceMessage(PresenceAction.UPDATE, isSubscriber = false)
        val initialProperties = createPublisherProperties()

        // when
        worker.doWork(
            initialProperties,
            asyncWorks.appendWork(),
            postedWorks.appendSpecification()
        )

        // then
        Truth.assertThat(asyncWorks).isEmpty()
        Truth.assertThat(postedWorks).isEmpty()

        verify(exactly = 0) {
            publisherInteractor.updateSubscriber(any(), trackable, any(), any())
        }
    }

    private fun prepareWorkerWithPresenceMessage(action: PresenceAction, isSubscriber: Boolean) =
        PresenceMessageWorker(
            trackable,
            PresenceMessage(
                action,
                PresenceData(if (isSubscriber) ClientTypes.SUBSCRIBER else ClientTypes.PUBLISHER),
                "test-member-key"
            ),
            publisherInteractor
        )
}
