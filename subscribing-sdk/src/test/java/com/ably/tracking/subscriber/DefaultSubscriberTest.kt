package com.ably.tracking.subscriber

import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.mockCreateConnectionSuccess
import com.ably.tracking.test.common.mockGetCurrentPresenceSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSubscriberTest {
    private val ably = mockk<Ably>(relaxed = true)
    private val trackableId = UUID.randomUUID().toString()
    private val subscriber = DefaultSubscriber(ably, null, trackableId, null)

    @Test
    fun `should not return an error when starting the subscriber with subscribing to presence error`() =
        runTest {
            // given
            ably.mockCreateConnectionSuccess(trackableId)
            ably.mockGetCurrentPresenceSuccess(trackableId)
            ably.mockSubscribeToPresenceError(trackableId)

            // when
            subscriber.start()

            // then
        }

    @Test
    fun `should not disconnect from the channel when starting the subscriber with subscribing to presence error`() =
        runTest {
            // given
            ably.mockCreateConnectionSuccess(trackableId)
            ably.mockGetCurrentPresenceSuccess(trackableId)
            ably.mockSubscribeToPresenceError(trackableId)

            // when
            subscriber.start()

            // then
        }
}
