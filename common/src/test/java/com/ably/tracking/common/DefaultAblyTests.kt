package com.ably.tracking.common

import com.ably.tracking.common.helper.DefaultAblyTestEnvironment
import io.mockk.verify
import io.mockk.verifyOrder
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test

class DefaultAblyTests {
    // This is just an example test to check that the AblySdkRealtime mocks are working correctly. We need to add a full set of unit tests for DefaultAbly; see https://github.com/ably/ably-asset-tracking-android/issues/869
    @Test
    fun `connect fetches the channel and then enters presence on it, and when that succeeds the call to connect succeeds`() {
        // Given
        val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
        val configuredChannel = testEnvironment.configuredChannels[0]
        testEnvironment.mockChannelsContainsKey(key = configuredChannel.channelName, result = false)
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

    @Test
    fun `close - behaviour when wrapped in a withTimeout block, when the timeout elapses during the presence leave operation`() {
        // Given...
        // ...that the Realtime instance has 1 channel...
        val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
        val configuredChannel = testEnvironment.configuredChannels[0]

        configuredChannel.mockName()
        // ...which is in the ATTACHED state...
        configuredChannel.mockState(ChannelState.attached)

        // ...and which, when asked to leave presence, never finishes doing so...
        configuredChannel.mockNonCompletingPresenceLeave()

        testEnvironment.mockChannelsEntrySet()

        // When...
        var caughtTimeoutCancellationException = false
        runBlocking {
            try {
                // ... we call `close` on the object under test within a withTimeout block, whose timeMillis is chosen arbitrarily but hopefully large enough so that internally, the call to `close` gets as far as telling the channel to leave presence...
                withTimeout(1000) {
                    testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))
                }
            } catch (_: TimeoutCancellationException) {
                caughtTimeoutCancellationException = true
            }
        }

        // Then...
        verifyOrder {
            // ...the channel is told to leave presence...
            configuredChannel.presenceMock.leave(any(), any())
        }

        // ...and the withTimeout call raises a TimeoutCancellationException.
        Assert.assertTrue(caughtTimeoutCancellationException)

        /* A note on why this test exists:
         *
         * 1. We have removed the ability for users to directly specify a timeout for the Publisher.stop() operation via a parameter on the `stop` method call, and our UPGRADING.md guide recommends that users instead wrap the call to `stop` in a `withTimeout` block. I wanted to check that this works...
         *
         * 2. ...specifically, I was a little unsure about whether it would work because I had seen, under certain circumstances which I now don’t remember, TimeoutCancellationException errors being caught by a `catch (e: Exception)` block in the internals of DefaultAbly (in an area related to discarding exceptions thrown by the presence leave operation), and not reaching the caller of withTimeout, which I remember finding surprising. I am being vague — and probably misguided and paranoid — here because I don't have much of an understanding of exactly how timeouts (and more generally, cancellation) work in Kotlin coroutines. But I wanted to be sure that the TimeoutCancellationException would indeed find its way to the caller of withTimeout.
         */
    }
}
