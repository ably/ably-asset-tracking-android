package com.ably.tracking.subscriber

import com.ably.tracking.Accuracy
import com.ably.tracking.ErrorInformation
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPublisherPresenceTest {

    private val mockMessageProcessor = mockk<PublisherPresenceMessageProcessor>(relaxed = true)

    companion object {
        private val mockPresenceData = PresenceData(ClientTypes.PUBLISHER, Resolution(Accuracy.BALANCED, 123, 1.2))

        /**
         * If the current overall state is PRESENT, then the lastSeen of any PRESENT publishers will be "now". So we have to use an approximation.
         *
         * If the overall state is anything else, or if the publisher is ABSENT - then we expect exact timestamps based on presence messages or the time
         * the connection went offline, and we can assert that exact match.
         */
        private fun knownPublisherTimestampsMatch(overallState: PublisherPresenceState, expected: KnownPublisher, actual: KnownPublisher): Boolean {
            if (
                (overallState == PublisherPresenceState.PRESENT || overallState == PublisherPresenceState.UNKNOWN) &&
                expected.state == LastKnownPublisherState.PRESENT
            ) {
                return abs(expected.lastSeen - actual.lastSeen) < 5000
            }

            return expected.lastSeen == actual.lastSeen
        }

        /**
         * Compare two known publisher matches for equality.
         */
        private fun knownPublisherMatches(overallState: PublisherPresenceState, expected: KnownPublisher, actual: KnownPublisher): Boolean =
            expected.state == actual.state &&
                expected.clientId == actual.clientId &&
                expected.memberKey == actual.memberKey &&
                expected.connectionId == actual.connectionId &&
                knownPublisherTimestampsMatch(overallState, expected, actual)

        /**
         * Check that the list of known publishers matches.
         */
        private fun knownPublishersMatches(expected: PublisherPresenceStateChange, actual: PublisherPresenceStateChange): Boolean {
            if (expected.publishers.size != actual.publishers.size) {
                return false
            }

            expected.publishers.forEachIndexed { index, knownPublisher ->
                if (!knownPublisherMatches(expected.state, knownPublisher, actual.publishers[index])) {
                    return false
                }
            }

            return true
        }

        /**
         * Compare two state changes for equality.
         */
        private fun stateChangeMatchesExpected(expected: PublisherPresenceStateChange, actual: PublisherPresenceStateChange): Boolean =
            expected.state == actual.state &&
            expected.reason == actual.reason &&
            abs(expected.timestamp - actual.timestamp) < 5000 &&
            knownPublishersMatches(expected, actual)

        /**
         * Wait for a given value to be emitted on the StateFlow.
         */
        private suspend fun assertValueEmission(publisherPresence: DefaultPublisherPresence, expectedStateChange: PublisherPresenceStateChange, scope: CoroutineScope) {
            val expectedValueEmitted = AtomicBoolean()
            val awaitingJob = publisherPresence.stateChanges.onEach {
                if (stateChangeMatchesExpected(expectedStateChange, it)) {
                    expectedValueEmitted.set(true)
                }
            }.launchIn(scope)


            val startTime = Date().time
            while (!expectedValueEmitted.get() && Date().time < startTime + 10000) {
                delay(500)
            }

            awaitingJob.cancel()
            assertThat(expectedValueEmitted.get()).isTrue()
        }

        /**
         * Wait for a bit to check that no state updates, except for an "allowed" state, are emitted.
         */
        private suspend fun assertNoValueEmission(publisherPresence: DefaultPublisherPresence, allowedStateChange: PublisherPresenceStateChange, scope: CoroutineScope) {
            val awaitingJob = publisherPresence.stateChanges.onEach {
                if (!stateChangeMatchesExpected(allowedStateChange, it)) {
                    throw AssertionError("Did not expect a value to be emitted")
                }
            }.launchIn(scope)


            val startTime = Date().time
            while (Date().time < startTime + 10000) {
                delay(500)
            }

            awaitingJob.cancel()
        }
    }

    @Test
    fun itEmitsADefaultValueOntoTheStateflow() = runTest {
        val expectedStateChange = PublisherPresenceStateChange(PublisherPresenceState.UNKNOWN, ErrorInformation(
            code = PublisherStateUnknownReasons.SUBSCRIBER_NEVER_ONLINE.value,
            statusCode = 0,
            message = "Subscriber has never been online",
            href = null,
            cause = null
        ), Date().time, listOf())

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        assertValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isTrue()
    }

    @Test
    fun itEmitsEventsForNewPublishers() = runTest {
        val expectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)

        assertValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itEmitsEventsForAbsentPublishers() = runTest {
        val expectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.ABSENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.ABSENT,
                    123
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)

        assertValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itEmitsAnOverallStatusOfPresentIfAtLeastOneKnownPublisher() = runTest {
        val expectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.ABSENT,
                    123
                ),
                KnownPublisher(
                    "ghi:jkl",
                    "jkl",
                    "ghi",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)

        assertValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itUpdatesPublishersAndEmitsChanges() = runTest {
        val firstExpectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.ABSENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.ABSENT,
                    123
                ),
                KnownPublisher(
                    "ghi:jkl",
                    "jkl",
                    "ghi",
                    LastKnownPublisherState.ABSENT,
                    234
                )
            )
        )

        val secondExpectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                ),
                KnownPublisher(
                    "ghi:jkl",
                    "jkl",
                    "ghi",
                    LastKnownPublisherState.ABSENT,
                    234
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)
        val firstMessages = listOf(
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 234,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )

        val secondMessages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages) } returns firstMessages
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages) } returns secondMessages
        publisherPresence.processPresenceMessages(firstMessages)
        assertValueEmission(publisherPresence, firstExpectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        publisherPresence.processPresenceMessages(secondMessages)
        assertValueEmission(publisherPresence, secondExpectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages)
            mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages)
        }
    }

    @Test
    fun itDoesNotEmitUpdatesIfThereAreNoChangesToUnderlyingPublisherStateAndTheOverallStateHasntChanged() = runTest {
        val initialStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)
        val firstMessages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            )
        )

        val secondMessages = listOf(
            PresenceMessage(
                action = PresenceAction.UPDATE,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages) } returns firstMessages
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages) } returns secondMessages
        publisherPresence.processPresenceMessages(firstMessages)
        assertValueEmission(publisherPresence, initialStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        publisherPresence.processPresenceMessages(secondMessages)
        assertNoValueEmission(publisherPresence, initialStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages)
            mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages)
        }
    }

    @Test
    fun itEmitsAStateChangeIfTransitioningFromUnknownIfNoNewNessages() = runTest {
        val initialStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)
        val firstMessages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            )
        )

        val secondMessages = listOf<PresenceMessage>()
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages) } returns firstMessages
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages) } returns secondMessages
        publisherPresence.processPresenceMessages(firstMessages)
        assertValueEmission(publisherPresence, initialStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        publisherPresence.connectionOffline()


        val secondStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )
        publisherPresence.processPresenceMessages(secondMessages)
        assertValueEmission(publisherPresence, secondStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages)
            mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages)
        }
    }

    @Test
    fun itEmitsAStateChangeIfTransitioningFromUnknownEvenIfPublishersHaveNotChanged() = runTest {
        val initialStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)
        val firstMessages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            )
        )

        val secondMessages = listOf(
            PresenceMessage(
                action = PresenceAction.UPDATE,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages) } returns firstMessages
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages) } returns secondMessages
        publisherPresence.processPresenceMessages(firstMessages)
        assertValueEmission(publisherPresence, initialStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        publisherPresence.connectionOffline()


        val secondStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.PRESENT,
            null,
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                )
            )
        )
        publisherPresence.processPresenceMessages(secondMessages)
        assertValueEmission(publisherPresence, secondStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(firstMessages)
            mockMessageProcessor.processPresenceMessagesAndGetChanges(secondMessages)
        }
    }

    @Test
    fun itHasPresentPublishersIfAtLeastOneKnownPublisherIsPresent() = runTest {
        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)

        assertThat(publisherPresence.hasPresentPublishers()).isTrue()
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itDoesntHavePresentPublishersIfNonePresent() = runTest {
        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)

        assertThat(publisherPresence.hasPresentPublishers()).isFalse()
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itEmitsAStateChangeWhenTheConnectionIsOfflineWherePresentPublishersHaveLastSeenAsNow() = runTest {

        val expectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.UNKNOWN,
            ErrorInformation(
                code = PublisherStateUnknownReasons.SUBSCRIBER_NOT_ONLINE.value,
                statusCode = 0,
                message = "Subscriber is not online",
                href = null,
                cause = null
            ),
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time + 5000 // We'll put in a delay to simulate time passing
                ),
                KnownPublisher(
                    "ghi:jkl",
                    "jkl",
                    "ghi",
                    LastKnownPublisherState.ABSENT,
                    234
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 234,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()

        // Simulate some time passing between the original presence messages and the connection going offline
        delay(5000)

        publisherPresence.connectionOffline()
        assertValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isTrue()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }

    @Test
    fun itDoesntEmitStateChangesIfConnectionAlreadyOffline() = runTest {

        val expectedStateChange = PublisherPresenceStateChange(
            PublisherPresenceState.UNKNOWN,
            ErrorInformation(
                code = PublisherStateUnknownReasons.SUBSCRIBER_NOT_ONLINE.value,
                statusCode = 0,
                message = "Subscriber is not online",
                href = null,
                cause = null
            ),
            Date().time,
            listOf(
                KnownPublisher(
                    "abc:def",
                    "def",
                    "abc",
                    LastKnownPublisherState.PRESENT,
                    Date().time
                ),
                KnownPublisher(
                    "ghi:jkl",
                    "jkl",
                    "ghi",
                    LastKnownPublisherState.ABSENT,
                    234
                )
            )
        )

        val publisherPresence = DefaultPublisherPresence(mockMessageProcessor, this)

        val messages = listOf(
            PresenceMessage(
                action = PresenceAction.PRESENT_OR_ENTER,
                data = mockPresenceData,
                timestamp = 123,
                memberKey = "abc:def",
                connectionId = "abc",
                clientId = "def",
                id = "abc:0:2"
            ),
            PresenceMessage(
                action = PresenceAction.LEAVE_OR_ABSENT,
                data = mockPresenceData,
                timestamp = 234,
                memberKey = "ghi:jkl",
                connectionId = "ghi",
                clientId = "jkl",
                id = "ghi:0:2"
            )
        )
        every { mockMessageProcessor.processPresenceMessagesAndGetChanges(messages) } returns messages
        publisherPresence.processPresenceMessages(messages)
        assertThat(publisherPresence.lastStateIsUnknown()).isFalse()
        publisherPresence.connectionOffline()
        assertThat(publisherPresence.lastStateIsUnknown()).isTrue()

        // Simulate some time passing between the original connection offline and now
        delay(5000)

        publisherPresence.connectionOffline()
        assertNoValueEmission(publisherPresence, expectedStateChange, this)
        assertThat(publisherPresence.lastStateIsUnknown()).isTrue()

        verify(exactly = 1) {
            mockMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        }
    }
}
