package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PresenceMessageWorkerTest {
    private lateinit var worker: PresenceMessageWorker
    private val trackable = Trackable("test-trackable")
    private val corePublisher = mockk<CorePublisher>(relaxed = true)
    private val publisherProperties = mockk<PublisherProperties>(relaxed = true)

    @Before
    fun setUp() {
        prepareWorkerWithPresenceMessage(PresenceAction.PRESENT_OR_ENTER, isSubscriber = true)
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
    fun `should add subscriber when presence action is enter and type is subscriber`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.PRESENT_OR_ENTER, isSubscriber = true)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.addSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should not add subscriber when presence action is enter but type is publisher`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.PRESENT_OR_ENTER, isSubscriber = false)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.addSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should remove subscriber when presence action is leave and type is subscriber`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.LEAVE_OR_ABSENT, isSubscriber = true)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.removeSubscriber(any(), trackable, any())
        }
    }

    @Test
    fun `should not remove subscriber when presence action is leave but type is publisher`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.LEAVE_OR_ABSENT, isSubscriber = false)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.removeSubscriber(any(), trackable, any())
        }
    }

    @Test
    fun `should update subscriber when presence action is update and type is subscriber`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.UPDATE, isSubscriber = true)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 1) {
            corePublisher.updateSubscriber(any(), trackable, any(), any())
        }
    }

    @Test
    fun `should not update subscriber when presence action is update but type is publisher`() {
        // given
        prepareWorkerWithPresenceMessage(PresenceAction.UPDATE, isSubscriber = false)

        // when
        worker.doWork(publisherProperties)

        // then
        verify(exactly = 0) {
            corePublisher.updateSubscriber(any(), trackable, any(), any())
        }
    }

    private fun prepareWorkerWithPresenceMessage(action: PresenceAction, isSubscriber: Boolean) {
        worker = PresenceMessageWorker(
            trackable,
            PresenceMessage(
                action,
                PresenceData(if (isSubscriber) ClientTypes.SUBSCRIBER else ClientTypes.PUBLISHER),
                "test-member-key"
            ),
            corePublisher
        )
    }
}
