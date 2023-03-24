package com.ably.tracking.subscriber

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultPublisherPresenceMessageProcessorTest {

    companion object {
        private val mockPresenceData = PresenceData(ClientTypes.PUBLISHER, Resolution(Accuracy.BALANCED, 123, 1.2))
        private val mockSubscriberPresenceData = PresenceData(ClientTypes.SUBSCRIBER, Resolution(Accuracy.BALANCED, 123, 1.2))
    }

    @Test
    fun itIgnoresSubscriberMessages() {

        val message1 = buildPresenceMessage(memberKey = "abc:def", data = mockSubscriberPresenceData)
        val message2 = buildPresenceMessage(memberKey = "abc:def", action = PresenceAction.LEAVE_OR_ABSENT, data = mockSubscriberPresenceData)
        val messages = listOf(message1, message2)

        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).isEmpty()
    }

    @Test
    fun itProcessesFirstMessageForAPublisherAndReturnsIt() {

        val message = buildPresenceMessage(memberKey = "abc:def")

        val messages = listOf(message)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message))
    }

    @Test
    fun itSupersedesMessagesIfTheyAreNewerForSamePublisherAndReturnsThem() {

        val message1 = buildPresenceMessage(memberKey = "abc:def")
        val message2 = buildPresenceMessage(memberKey = "abc:def", messageIndex = 2)

        val messages = listOf(message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message2))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreTheSameAndDoesNotReturnThem() {

        val message1 = buildPresenceMessage(memberKey = "abc:def")
        val message2 = buildPresenceMessage(memberKey = "abc:def")

        val messages = listOf(message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreForDifferentPublisherAndReturnsMessages() {

        val message1 = buildPresenceMessage(memberKey = "abc:def", messageIndex = 2)
        val message2 = buildPresenceMessage(memberKey = "abc:ghi")
        val message3 = buildPresenceMessage(memberKey = "abc:jkl")

        val messages = listOf(message3, message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1, message2, message3))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreOlderForSamePublisherAndReturnsThem() {

        val message1 = buildPresenceMessage(memberKey = "abc:def", messageIndex = 2)
        val message2 = buildPresenceMessage(memberKey = "abc:def")
        val message3 = buildPresenceMessage(memberKey = "abc:def")

        val messages = listOf(message3, message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1))
    }

    @Test
    fun itPersistsStateBetweenUpdatesAndReturnsNewMessagesOnEachCall() {

        // First time this message is seen, but superseded by 1c, will not be seen
        val message1a = buildPresenceMessage(memberKey = "abc:def", messageIndex = 2)

        // Not as "new" as 1a, will not be seen in output of first call
        val message1b = buildPresenceMessage(memberKey = "abc:def")

        // Newer than 1a, will be in output of first call. Will not be re-emitted in second.
        val message1c = buildPresenceMessage(memberKey = "abc:def", messageIndex = 3)

        // Not as new as 1c, will not be emitted in second call
        val message1d = buildPresenceMessage(memberKey = "abc:def")

        // Will be emitted in first call
        val message2a = buildPresenceMessage(memberKey = "abc:ghi")

        // Will be emitted in second call
        val message2b = buildPresenceMessage(memberKey = "abc:ghi", messageIndex = 2)

        // First seen in second call, will be emitted
        val message3 = buildPresenceMessage(memberKey = "abc:jkl")

        val firstMessages = listOf(message1a, message2a, message1c, message1b)
        val secondMessages = listOf(message1c, message2b, message1d, message3)
        val processor = DefaultPublisherPresenceMessageProcessor()

        assertThat(processor.processPresenceMessagesAndGetChanges(firstMessages)).containsExactlyElementsIn(listOf(message1c, message2a))
        assertThat(processor.processPresenceMessagesAndGetChanges(secondMessages)).containsExactlyElementsIn(listOf(message2b, message3))
    }

    private fun buildPresenceMessage(
        memberKey: String = "abc:def",
        action: PresenceAction = PresenceAction.PRESENT_OR_ENTER,
        messageIndex: Int = 1,
        data: PresenceData = mockPresenceData
    ) = PresenceMessage(
        action = action,
        data = data,
        timestamp = 123,
        memberKey = memberKey,
        connectionId = memberKey.split(':', limit = 2)[0],
        clientId = memberKey.split(':', limit = 2)[1],
        id = "abc:0:${messageIndex}"
    )
}
