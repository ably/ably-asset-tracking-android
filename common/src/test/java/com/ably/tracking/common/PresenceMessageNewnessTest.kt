package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PresenceMessageNewnessTest(val testName: String, val firstMessage: PresenceMessage, val secondMessage: PresenceMessage, val shouldBeNewer: Boolean) {

    companion object {

        private val mockPresenceData = PresenceData(ClientTypes.PUBLISHER, Resolution(Accuracy.BALANCED, 123, 1.2))

        /**
         * Some definitions:
         *
         * A "synthesized leave" message is defined RTP2b1
         */
        @JvmStatic
        @Parameterized.Parameters(name = "Given two messages where {0}, isNewerThan returns {3}")
        fun data() = arrayOf(
            arrayOf(
                "first message is synthesized leave and has a greater timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                true
            ),
            arrayOf(
                "second message is synthesized leave and first has a greater timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                true
            ),
            arrayOf(
                "both messages are synthesized leave and first has a greater timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                true
            ),
            arrayOf(
                "first message is synthesized leave and has a smaller timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "second message is synthesized leave and first has a smaller timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "both messages are synthesized leave and first has a smaller timestamp",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abd",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the first message has an too many id parts",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1:2"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the first message has too few id parts",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the second message has too many id parts",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1:2"
                ),
                false
            ),
            arrayOf(
                "the second message has too few id parts",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0"
                ),
                false
            ),
            arrayOf(
                "the first message serial is non numeric",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:a:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the first message serial is not an integer",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:1.1:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the first message index is non numeric",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:a"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the first message index is not an integer",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1.9"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                false
            ),
            arrayOf(
                "the second message serial is non numeric",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:a:1"
                ),
                false
            ),
            arrayOf(
                "the second message serial is not an integer",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:1.2:1"
                ),
                false
            ),
            arrayOf(
                "the second message index is not numeric",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:a"
                ),
                false
            ),
            arrayOf(
                "the second message index is not an integer",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1.2"
                ),
                false
            ),
            arrayOf(
                "the second message serial is greater than the first",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:2"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:1:1"
                ),
                false
            ),
            arrayOf(
                "the second message serial is equal to the first, but its index is higher",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:2"
                ),
                false
            ),
            arrayOf(
                "the first message has a higher message serial",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:2:1"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:0:4"
                ),
                true
            ),
            arrayOf(
                "second message serial is equal to the first, but its index is smaller",
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 100,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:2:4"
                ),
                PresenceMessage(
                    action = PresenceAction.LEAVE_OR_ABSENT,
                    data = mockPresenceData,
                    timestamp = 123,
                    memberKey = "abc:def",
                    connectionId = "abc",
                    clientId = "def",
                    id = "abc:2:3"
                ),
                true
            ),
        )
    }

    @Test
    fun presenceMessagesCanBeComparedForNewness() {
        assertThat(firstMessage.isNewerThan(secondMessage)).isEqualTo(shouldBeNewer)
    }
}
