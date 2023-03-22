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
        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockSubscriberPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:1"
            ),
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockSubscriberPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:1"
            )
        )

        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).isEmpty()
    }

    @Test
    fun itProcessesFirstMessageForAPublisherAndReturnsIt() {

        val message = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val messages = listOf(message)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message))
    }

    @Test
    fun itSupersedesMessagesIfTheyAreNewerForSamePublisherAndReturnsThem() {

        val message1 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val message2 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        val messages = listOf(message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message2))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreTheSameAndDoesNotReturnThem() {

        val message1 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val message2 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val messages = listOf(message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreForDifferentPublisherAndReturnsMessages() {

        val message1 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        val message2 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:ghi",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val message3 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:jkl",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val messages = listOf(message3, message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1, message2, message3))
    }

    @Test
    fun itDoesNotSupersedeMessagesIfTheyAreOlderForSamePublisherAndReturnsThem() {

        val message1 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        val message2 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val message3 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        val messages = listOf(message3, message1, message2)
        assertThat(DefaultPublisherPresenceMessageProcessor().processPresenceMessagesAndGetChanges(messages)).containsExactlyElementsIn(listOf(message1))
    }

    @Test
    fun itPersistsStateBetweenUpdatesAndReturnsNewMessagesOnEachCall() {

        // First time this message is seen, but superseded by 1c, will not be seen
        val message1a = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        // Not as "new" as 1a, will not be seen in output of first call
        val message1b = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        // Newer than 1a, will be in output of first call. Will not be re-emitted in second.
        val message1c = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:3"
        )

        // Not as new as 1c, will not be emitted in second call
        val message1d = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:def",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        // Will be emitted in first call
        val message2a = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:ghi",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:1"
        )

        // Will be emitted in second call
        val message2b = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:ghi",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        // First seen in second call, will be emitted
        val message3 = PresenceMessage(
            action = PresenceAction.PRESENT_OR_ENTER,
            data = mockPresenceData,
            timestamp = 123,
            memberKey = "abc:jkl",
            connectionId = "abc",
            clientId = "def",
            id = "abc:0:2"
        )

        val firstMessages = listOf(message1a, message2a, message1c, message1b)
        val secondMessages = listOf(message1c, message2b, message1d, message3)
        val processor = DefaultPublisherPresenceMessageProcessor()

        assertThat(processor.processPresenceMessagesAndGetChanges(firstMessages)).containsExactlyElementsIn(listOf(message1c, message2a))
        assertThat(processor.processPresenceMessagesAndGetChanges(secondMessages)).containsExactlyElementsIn(listOf(message2b, message3))
    }
}
