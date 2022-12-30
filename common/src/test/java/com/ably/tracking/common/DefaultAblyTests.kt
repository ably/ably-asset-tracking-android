package com.ably.tracking.common

import com.ably.tracking.common.helper.DefaultAblyTestEnvironment
import io.mockk.verify
import io.mockk.verifyOrder
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DefaultAblyTests {
    // This is just an example test to check that the AblySdkRealtime mocks are working correctly. We need to add a full set of unit tests for DefaultAbly; see https://github.com/ably/ably-asset-tracking-android/issues/869
    @Test
    fun `connect fetches the channel and then enters presence on it, and when that succeeds the call to connect succeeds`() {
        // Given
        val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
        val configuredChannel = testEnvironment.configuredChannels[0]
        configuredChannel.mockSuccessfulPresenceEnter()

        // When
        runBlocking {
            testEnvironment.objectUnderTest.connect(configuredChannel.trackableId, PresenceData(""))
        }

        // Then
        verify {
            testEnvironment.channelsMock.get(configuredChannel.channelName, any())
            configuredChannel.presenceMock.enter(any(), any())
        }
    }

    @Test
    fun `close - behaviour when all presence leave calls succeed`() {
        // Given...
        // ...that the Realtime instance has 3 channels...
        val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 3)

        // ...and that each of these channels...
        for (configuredChannel in testEnvironment.configuredChannels) {
            configuredChannel.mockName()
            // ...is in the ATTACHED state...
            configuredChannel.mockState(ChannelState.attached)

            // ...and, when asked to leave presence, does so successfully...
            configuredChannel.mockSuccessfulPresenceLeave()
            configuredChannel.stubUnsubscribe()
            configuredChannel.stubPresenceUnsubscribe()
            testEnvironment.stubRelease(configuredChannel)
        }

        testEnvironment.mockChannelsEntrySet()

        // ...and the Realtime instance is in the CONNECTED state...
        testEnvironment.mockConnectionState(ConnectionState.connected)
        // ...and, when asked to close the connection, emits an error-free state change from CONNECTED to CLOSED,
        testEnvironment.mockCloseToEmitStateChange(
            previous = ConnectionState.connected,
            current = ConnectionState.closed,
            retryIn = 0,
            reason = null
        )
        testEnvironment.stubConnectionOff()

        // When...
        runBlocking {
            // ... we call `close` on the object under test,
            testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))
        }

        // Then...
        verifyOrder {
            // ...each of the channels...
            for (configuredChannel in testEnvironment.configuredChannels) {
                // ...is asked to leave presence....
                configuredChannel.presenceMock.leave(any(), any())
                // ...and then to unsubscribe from channel and presence messages...
                configuredChannel.channelMock.unsubscribe()
                configuredChannel.presenceMock.unsubscribe()
                // ...and then gets released...
                testEnvironment.channelsMock.release(configuredChannel.channelName)
            }
            // ...and the Realtime instance is asked to close the connection...
            // (Note that although it's a desired behaviour, we are not here testing that the Realtime instance is only asked to close the connection _after_ the call to leave presence have finished execution. Because it seemed like it would make the test more complicated.)
            testEnvironment.realtimeMock.close()
        }

        // ...and (implicitly from the When), the call to `close` (on the object under test) succeeds.
    }

    @Test
    fun `close - behaviour when some presence leave calls succeed and others fail`() {
        // Given...
        // ...that the Realtime instance has 3 channels...
        val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 3)

        testEnvironment.configuredChannels.forEachIndexed { i, configuredChannel ->
            configuredChannel.mockName()
            // ...and that each of these channels is in the ATTACHED state...
            configuredChannel.mockState(ChannelState.attached)

            // ...and that, when asked to leave presence, one of the channels fails to do so and the other two do so successfully...
            if (i == 0) {
                configuredChannel.mockFailedPresenceLeave(
                    ErrorInfo(
                        "abc",
                        500 /* both values arbitrarily chosen */
                    )
                )
            } else {
                configuredChannel.mockSuccessfulPresenceLeave()
            }
            configuredChannel.stubUnsubscribe()
            configuredChannel.stubPresenceUnsubscribe()
            testEnvironment.stubRelease(configuredChannel)
        }

        testEnvironment.mockChannelsEntrySet()

        // ...and the Realtime instance is in the CONNECTED state...
        testEnvironment.mockConnectionState(ConnectionState.connected)
        // ...and, when asked to close the connection, emits an error-free state change from CONNECTED to CLOSED,
        testEnvironment.mockCloseToEmitStateChange(
            previous = ConnectionState.connected,
            current = ConnectionState.closed,
            retryIn = 0,
            reason = null
        )
        testEnvironment.stubConnectionOff()

        // When...
        runBlocking {
            // ... we call `close` on the object under test,
            testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))
        }

        // Then...
        verifyOrder {
            testEnvironment.configuredChannels.forEachIndexed { i, configuredChannel ->
                // ...each of the channels is asked to leave presence...
                configuredChannel.presenceMock.leave(any(), any())

                if (i != 0) {
                    // ...and each of the channels which successfully left presence is then asked to unsubscribe from channel and presence messages...
                    configuredChannel.channelMock.unsubscribe()
                    configuredChannel.presenceMock.unsubscribe()
                    // ...and then gets released...
                    testEnvironment.channelsMock.release(configuredChannel.channelName)
                }
            }
            // ...and the Realtime instance is asked to close the connection...
            // (Note that although it's a desired behaviour, we are not here testing that the Realtime instance is only asked to close the connection _after_ all of the calls to leave presence have finished execution. Because it seemed like it would make the test more complicated.)
            testEnvironment.realtimeMock.close()
        }

        // ...and (implicitly from the When), the call to `close` (on the object under test) succeeds.
    }
}
