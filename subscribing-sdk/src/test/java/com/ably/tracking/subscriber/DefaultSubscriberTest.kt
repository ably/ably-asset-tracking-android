package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.mockCreateConnectionSuccess
import com.ably.tracking.test.common.mockDisconnectSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DefaultSubscriberTest {
    private val ably = mockk<Ably>(relaxed = true)
    private val trackableId = UUID.randomUUID().toString()
    private val subscriber = DefaultSubscriber(ably, null, trackableId, null)

    @Test(expected = ConnectionException::class)
    fun `should return an error when starting the subscriber with subscribing to presence error`() {
        // given
        ably.mockCreateConnectionSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        runBlocking {
            subscriber.start()
        }

        // then
    }

    @Test()
    fun `should disconnect from the channel when starting the subscriber with subscribing to presence error`() {
        // given
        ably.mockCreateConnectionSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        runBlocking {
            try {
                subscriber.start()
            } catch (exception: ConnectionException) {
                // ignoring exception in this test
            }
        }

        // then
        verify(exactly = 1) {
            ably.disconnect(trackableId, any(), any())
        }
    }

    @Test()
    fun `should not listen for the raw locations if they are disabled`() {
        // given
        val subscriber = DefaultSubscriber(ably, null, trackableId, false)
        ably.mockCreateConnectionSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        runBlocking {
            try {
                subscriber.start()
            } catch (exception: ConnectionException) {
                // ignoring exception in this test
            }
        }

        // then
        verify(exactly = 0) {
            ably.subscribeForRawEvents(trackableId, any())
        }
    }

    @Test()
    fun `should listen for the raw locations if they are enabled`() {
        // given
        val subscriber = DefaultSubscriber(ably, null, trackableId, true)
        ably.mockCreateConnectionSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        runBlocking {
            try {
                subscriber.start()
            } catch (exception: ConnectionException) {
                // ignoring exception in this test
            }
        }

        // then
        verify(exactly = 1) {
            ably.subscribeForRawEvents(trackableId, any())
        }
    }
}
