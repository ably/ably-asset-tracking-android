package com.ably.tracking.subscriber

import com.ably.tracking.ErrorInformation
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
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
        private fun stateChangeMatchesExpected(expected: PublisherPresenceStateChange, actual: PublisherPresenceStateChange): Boolean =
            expected.state == actual.state &&
            expected.reason == actual.reason &&
            abs(expected.timestamp - actual.timestamp) < 3000 &&
            expected.publishers == actual.publishers

        private suspend fun waitForValueEmission(publisherPresence: DefaultPublisherPresence, expectedStateChange: PublisherPresenceStateChange, scope: CoroutineScope): Boolean {
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
            return expectedValueEmitted.get()
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

        assertThat(waitForValueEmission(publisherPresence, expectedStateChange, this)).isTrue()
    }
}
