package com.ably.tracking.common

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.helper.DefaultAblyTestEnvironment
import com.ably.tracking.common.helper.DefaultAblyTestScenarios
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.ErrorInfo
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test

/**
 * # Unit tests for [DefaultAbly]
 *
 * Here are some notes about the approach taken for testing [DefaultAbly].
 *
 * ## Documenting the absence of built-in timeout
 *
 * Whilst writing black-box tests for this class, I wanted to document which parts of DefaultAbly have a built-in timeout behaviour, and which don't. So, I’ve written some tests which examine how this class’s methods behave when an underlying Ably SDK operation runs indefinitely. In the case where we wish to document that there is no built-in timeout behaviour, these tests aren't really _testing_ anything – there’s no good way to prove that something doesn’t have a timeout other than reading the implementation. So, these tests just demonstrate that in the absence of activity from the Ably SDK, the [DefaultAbly] method will not return within [noTimeoutDemonstrationWaitingTimeInMilliseconds]. If you want to convince yourself that there really is no built-in timeout, you can set [noTimeoutDemonstrationWaitingTimeInMilliseconds] to a larger number and run the tests.
 *
 * If we remove all of the built-in timeout behaviour from DefaultAbly in the future then it’ll be fine to delete these tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAblyTests {
    /**
     * The arbitrarily-chosen number of milliseconds that a test will wait as a demonstration that a given operation does not have a built-in timeout. See "Demonstrating the absence of no built-in timeout" above.
     */
    private val noTimeoutDemonstrationWaitingTimeInMilliseconds = 100L

    /*
    Observations from writing black-box tests for `connect`:

    - When given a channel in certain states, it seems to fetch the channel’s state more than once. I have not tested what happens if a different state is returned on the second call.
     */

    @Test
    fun `connect - when channel is not created and attach succeeds`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel...
         * ...which, when told to attach, does so successfully...
         *
         * When...
         *
         * ...we call `connect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that accepts a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to attach...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.Connect.test(
            DefaultAblyTestScenarios.Connect.GivenConfig(
                channelsContainsKey = false,
                channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success
            ),
            DefaultAblyTestScenarios.Connect.ThenConfig(
                verifyChannelAttach = true,
                overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                verifyChannelRelease = false,
                resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `connect - when channel is not created and attach fails`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel...
         * ...which, when told to attach, fails to do so with an arbitrarily-chosen fatal error `attachError`...
         *
         * When...
         *
         * ...we call `connect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that accepts a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to attach...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code`, `statusCode`, and `message` as `attachError`.
         */

        val attachError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            400, /* fatal status code */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.Connect.test(
            DefaultAblyTestScenarios.Connect.GivenConfig(
                channelsContainsKey = false,
                channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                    attachError
                )
            ),
            DefaultAblyTestScenarios.Connect.ThenConfig(
                overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                verifyChannelAttach = true,
                verifyChannelRelease = true,
                resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            attachError.code,
                            attachError.statusCode,
                            attachError.message,
                            attachError.code.toHref(),
                            null
                        )
                    )
                ),
            ),
            this
        )
    }

    private fun Int.toHref() = "https://help.ably.io/error/$this"

    @Test
    fun `connect - when channel attach does not complete`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel...
         * ...which, when told to attach, never finishes doing so...
         *
         * When...
         *
         * ...we call `connect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that accepts a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to attach...
         * ...and the call to `connect` (on the object under test) does not complete (see “Documenting the absence of built-in timeout” above).
         */

        DefaultAblyTestScenarios.Connect.test(
            DefaultAblyTestScenarios.Connect.GivenConfig(
                channelsContainsKey = false,
                channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete
            ),
            DefaultAblyTestScenarios.Connect.ThenConfig(
                overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                verifyChannelAttach = true,
                verifyChannelRelease = false,
                resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.DoesNotTerminate(
                    timeoutInMilliseconds = noTimeoutDemonstrationWaitingTimeInMilliseconds
                )
            ),
            this
        )
    }

    @Test
    fun `connect - when channel already exists`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel...
         *
         * When...
         *
         * ...we call `connect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.Connect.test(
            DefaultAblyTestScenarios.Connect.GivenConfig(
                channelsContainsKey = true,
                channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS,
                channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked
            ),
            DefaultAblyTestScenarios.Connect.ThenConfig(
                overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS,
                verifyChannelAttach = false,
                verifyChannelRelease = false,
                resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `enterChannelPresence - when presence enter succeeds`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel which, when told to enter presence, does so successfully...
         *
         * When...
         *
         * ...we call `enterChannelPresence` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to enter presence...
         * ...and the call to `enterChannelPresence` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.EnterChannelPresence.test(
            DefaultAblyTestScenarios.EnterChannelPresence.GivenConfig(
                channelsContainsKey = true,
                presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
            ),
            DefaultAblyTestScenarios.EnterChannelPresence.ThenConfig(
                verifyChannelsGet = true,
                verifyPresenceEnter = true,
                resultOfEnterChannelPresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `enterChannelPresence - when presence enter fails`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel which, when told to enter presence, fails to do so with arbitrarily-chosen error `presenceError`...
         *
         * When...
         *
         * ...we call `enterChannelPresence` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` on the Channels instance...
         * ...and tells the channel to enter presence...
         * ...and the call to `enterChannelPresence` (on the object under test) fails with a `ConnectionException` whose errorInformation has the same `code` and `message` as `presenceError`.
         */

        val presenceError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.EnterChannelPresence.test(
            DefaultAblyTestScenarios.EnterChannelPresence.GivenConfig(
                channelsContainsKey = true,
                presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                    presenceError
                ),
            ),
            DefaultAblyTestScenarios.EnterChannelPresence.ThenConfig(
                verifyChannelsGet = true,
                verifyPresenceEnter = true,
                resultOfEnterChannelPresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        errorInformation =
                        ErrorInformation(
                            code = presenceError.code,
                            statusCode = 0,
                            message = presenceError.message,
                            href = null,
                            cause = null
                        )
                    )
                )
            ),
            this
        )
    }

    @Test
    fun `enterChannelPresence - when channel does not exist`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         *
         * When...
         *
         * ...we call `enterChannelPresence` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and the call to `enterChannelPresence` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.EnterChannelPresence.test(
            DefaultAblyTestScenarios.EnterChannelPresence.GivenConfig(
                channelsContainsKey = false,
                presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
            ),
            DefaultAblyTestScenarios.EnterChannelPresence.ThenConfig(
                verifyChannelsGet = false,
                verifyPresenceEnter = false,
                resultOfEnterChannelPresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    /*
    Observations from writing black-box tests for `updatePresenceData`:

    - When given a channel in certain states, it seems to fetch the channel’s state more than once. I have not tested what happens if a different state is returned on the second call.
     */

    @Test
    fun `updatePresenceData - when presence update succeeds`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel...
         * ...which, when told to update presence data, does so successfully,
         *
         * When...
         *
         * ...we call `updatePresenceData` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to update presence data...
         * ...and the call to `updatePresenceData` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.UpdatePresenceData.test(
            DefaultAblyTestScenarios.UpdatePresenceData.GivenConfig(
                channelsContainsKey = true,
                mockChannelsGet = true,
                presenceUpdateBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success
            ),
            DefaultAblyTestScenarios.UpdatePresenceData.ThenConfig(
                verifyChannelsGet = true,
                verifyGetChannelName = false,
                verifyPresenceUpdate = true,
                resultOfUpdatePresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `updatePresenceData - when presence update fails`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel...
         * ...which, when told to update presence data, fails to do so with arbitrarily-chosen error `presenceError`,
         *
         * When...
         *
         * ...we call `updatePresenceData` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to update presence data...
         * ...and the call to `updatePresenceData` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `presenceError`.
         */

        val presenceError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.UpdatePresenceData.test(
            DefaultAblyTestScenarios.UpdatePresenceData.GivenConfig(
                channelsContainsKey = true,
                mockChannelsGet = true,
                presenceUpdateBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                    presenceError
                )
            ),
            DefaultAblyTestScenarios.UpdatePresenceData.ThenConfig(
                verifyChannelsGet = true,
                verifyGetChannelName = false,
                verifyPresenceUpdate = true,
                resultOfUpdatePresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            presenceError.code,
                            0,
                            presenceError.message,
                            null,
                            null
                        )
                    )
                )
            ),
            this
        )
    }

    @Test
    fun `updatePresenceData - when channel doesn't exist`() = runTest {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         *
         * When...
         *
         * ...we call `updatePresenceData` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and the call to `updatePresenceData` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.UpdatePresenceData.test(
            DefaultAblyTestScenarios.UpdatePresenceData.GivenConfig(
                channelsContainsKey = false,
                mockChannelsGet = false,
                presenceUpdateBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked
            ),
            DefaultAblyTestScenarios.UpdatePresenceData.ThenConfig(
                verifyChannelsGet = false,
                verifyGetChannelName = false,
                verifyPresenceUpdate = false,
                resultOfUpdatePresenceCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `disconnect - when presence leave succeeds`() = runTest {
        /* Given...
         *
         * ...that calling containsKey on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel which, when told to leave presence, does so successfully...
         *
         * When...
         *
         * ...we call `disconnect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` on the Channels instance...
         * ...and tells the channel to leave presence...
         * ...and calls `unsubscribe` on the channel and on its Presence instance...
         * ...and fetches the channel’s name and calls `release` on the Channels instance...
         * ...and the call to `disconnect` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.Disconnect.test(
            DefaultAblyTestScenarios.Disconnect.GivenConfig(
                channelsContainsKey = true,
                presenceLeaveBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success
            ),
            DefaultAblyTestScenarios.Disconnect.ThenConfig(
                verifyChannelTeardown = true
            ),
            this
        )
    }

    @Test
    fun `disconnect - when presence leave fails`() = runTest {
        /* Given...
         *
         * ...that calling containsKey on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel which, when told to leave presence, fails to do so with an arbitrarily-chosen error `presenceError`...
         *
         * When...
         *
         * ...we call `disconnect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` on the Channels instance...
         * ...and tells the channel to leave presence...
         * ...and calls `unsubscribe` on the channel and on its Presence instance...
         * ...and fetches the channel’s name and calls `release` on the Channels instance...
         * ...and the call to `disconnect` (on the object under test) succeeds.
         */

        val presenceError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.Disconnect.test(
            DefaultAblyTestScenarios.Disconnect.GivenConfig(
                channelsContainsKey = true,
                presenceLeaveBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                    presenceError
                )
            ),
            DefaultAblyTestScenarios.Disconnect.ThenConfig(
                verifyChannelTeardown = true
            ),
            this
        )
    }

    @Test
    fun `disconnect - when presence leave does not complete`() = runTest {
        /*
         * Given...
         *
         * ...that calling containsKey on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel which, when told to leave presence, never finishes doing so...
         *
         * When...
         *
         * ...we call `disconnect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` on the Channels instance...
         * ...and tells the channel to leave presence...
         * ...and calls `unsubscribe` on the channel and on its Presence instance...
         * ...and fetches the channel’s name and calls `release` on the Channels instance...
         * ...and the call to `disconnect` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.Disconnect.test(
            DefaultAblyTestScenarios.Disconnect.GivenConfig(
                channelsContainsKey = true,
                presenceLeaveBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete
            ),
            DefaultAblyTestScenarios.Disconnect.ThenConfig(
                verifyChannelTeardown = true,
            ),
            this
        )
    }

    @Test
    fun `disconnect - when channel doesn't exist`() = runTest {
        /* Given...
         *
         * ...that calling containsKey on the Channels instance returns false...
         *
         * When...
         *
         * ...we call `disconnect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         * ...it calls `containsKey` on the Channels instance...
         * ...and the call to `disconnect` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.Disconnect.test(
            DefaultAblyTestScenarios.Disconnect.GivenConfig(
                channelsContainsKey = false,
                presenceLeaveBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked
            ),
            DefaultAblyTestScenarios.Disconnect.ThenConfig(
                verifyChannelTeardown = false,
            ),
            this
        )
    }

    /*
    Observations from writing black-box tests for `startConnection`:

    - When given a connection in certain states, it seems to fetch the connection’s state more than once. I have not tested what happens if a different state is returned on the second call.
     */

    @Test
    fun `startConnection - when connection is in INITIALIZED state, and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns INITIALIZED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is INITIALIZED, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.initialized,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.initialized,
                        current = ConnectionState.connected,
                        retryIn = 0, /* arbitrarily-chosen */
                        reason = null /* arbitrarily-chosen */
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in CONNECTING state and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CONNECTING...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CONNECTING, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.connecting,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.connecting,
                        current = ConnectionState.connected,
                        retryIn = 0, /* arbitrarily-chosen */
                        reason = null /* arbitrarily-chosen */
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in DISCONNECTED state and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns DISCONNECTED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is DISCONNECTED, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.disconnected,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.disconnected,
                        current = ConnectionState.connected,
                        retryIn = 0,
                        reason = null
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in SUSPENDED state and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns SUSPENDED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is SUSPENDED, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.suspended,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.suspended,
                        current = ConnectionState.connected,
                        retryIn = 0,
                        reason = null
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in CLOSING state and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CLOSING...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CLOSING, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.closing,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.closing,
                        current = ConnectionState.connected,
                        retryIn = 0,
                        reason = null
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in CLOSED state and, after connect called, changes to CONNECTED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CLOSED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CLOSED, `current` is CONNECTED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.closed,
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.closed,
                        current = ConnectionState.connected,
                        retryIn = 0,
                        reason = null
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                ),
                this
            )
        }

    @Test
    fun `startConnection - when connection is in FAILED state`() = runTest {
        /* Given...
         *
         * ...that the connection’s `state` property returns FAILED...
         * ...and that the connection’s `reason` property returns the arbitrarily-chosen error `connectionReason`...
         *
         * When...
         *
         * ...the `startConnection` method is called on the object under test...
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it fetches the connection’s state 2 times...
         * ...and fetches the connection’s `reason`...
         * ...and the call to `startConnection` (on the object under test) fails with a ConnectionException whose `code` and `message` are equal to those of `connectionReason`.
         */

        val connectionReason = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.StartConnection.test(
            DefaultAblyTestScenarios.StartConnection.GivenConfig(
                initialConnectionState = ConnectionState.failed,
                connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.Mocked(
                    connectionReason
                ),
                connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour
            ),
            DefaultAblyTestScenarios.StartConnection.ThenConfig(
                numberOfConnectionStateFetchesToVerify = 2,
                verifyConnectionReasonFetch = true,
                verifyConnectionOn = false,
                verifyConnect = false,
                verifyConnectionOff = false,
                resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                    ErrorInformation(
                        connectionReason.code,
                        0,
                        connectionReason.message,
                        null,
                        null
                    )
                )
            ),
            this
        )
    }

    @Test
    fun `startConnection - when connection is in CONNECTED state`() = runTest {
        /* Given...
         *
         * ...that the connection’s `state` property returns CONNECTED...
         *
         * When...
         *
         * ...the `startConnection` method is called on the object under test...
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it fetches the connection’s state once...
         * ...and the call to `startConnection` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.StartConnection.test(
            DefaultAblyTestScenarios.StartConnection.GivenConfig(
                initialConnectionState = ConnectionState.connected,
                connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour,
            ),
            DefaultAblyTestScenarios.StartConnection.ThenConfig(
                numberOfConnectionStateFetchesToVerify = 1,
                verifyConnectionReasonFetch = false,
                verifyConnectionOn = false,
                verifyConnect = false,
                verifyConnectionOff = false,
                resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
            ),
            this
        )
    }

    @Test
    fun `startConnection - when, after connect called, connection changes to FAILED state`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns (arbitrarily chosen) INITIALIZED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is INITIALIZED, `current` is FAILED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is the arbitrarily-chosen error `connectionError`...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) fails with a ConnectionException whose `code` and `message` are equal to those of `connectionError`.
             */

            val connectionError = ErrorInfo(
                "example of an error message", /* arbitrarily chosen */
                123 /* arbitrarily chosen */
            )

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.initialized, /* arbitrarily-chosen */
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.initialized,
                        current = ConnectionState.failed,
                        retryIn = 0,
                        reason = connectionError
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            connectionError.code,
                            0,
                            connectionError.message,
                            null,
                            null
                        )
                    )
                ),
                this
            )
        }

    @Test
    fun `startConnection - when, after connect called, connection changes to CLOSED state`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns (arbitrarily chosen) INITIALIZED...
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is INITIALIZED, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to connect...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `startConnection` (on the object under test) fails with a ConnectionException with `code` 100_000, `statusCode` 0, and `message` "Connection entered closed state whilst waiting for it to connect".
             */

            DefaultAblyTestScenarios.StartConnection.test(
                DefaultAblyTestScenarios.StartConnection.GivenConfig(
                    initialConnectionState = ConnectionState.initialized, /* arbitrarily-chosen */
                    connectionReasonBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionReasonMockBehaviour.NotMocked,
                    connectBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.initialized,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    ),
                ),
                DefaultAblyTestScenarios.StartConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 2,
                    verifyConnectionReasonFetch = false,
                    verifyConnectionOn = true,
                    verifyConnect = true,
                    verifyConnectionOff = true,
                    resultOfStartConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            100_000,
                            0,
                            "Connection entered closed state whilst waiting for it to connect",
                            null,
                            null
                        )
                    )
                ),
                this
            )
        }

    /*
    Observations from writing black-box tests for `stopConnection`:

    - When given a connection in certain states, it seems to fetch the connection’s state more than once. I have not tested what happens if a different state is returned on the second call.
    */

    @Test
    fun `stopConnection - when connection is in INITIALIZED state`() = runTest {
        /* Given...
         *
         * ...that the connection’s `state` property returns INITIALIZED...
         * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is INITIALIZED, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
         *
         * When...
         *
         * ...`stopConnection` is called on the object under test...
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it fetches the connection’s state 3 times...
         * ...and the call to `stopConnection` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.StopConnection.test(
            DefaultAblyTestScenarios.StopConnection.GivenConfig(
                initialConnectionState = ConnectionState.initialized,
                closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                    previous = ConnectionState.initialized,
                    current = ConnectionState.closed,
                    retryIn = 0,
                    reason = null
                )
            ),
            DefaultAblyTestScenarios.StopConnection.ThenConfig(
                numberOfConnectionStateFetchesToVerify = 3,
                verifyConnectionOn = false,
                verifyClose = false,
                verifyConnectionOff = false,
                resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `stopConnection - when connection is in CONNECTING state and, after close called, changes to CLOSED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CONNECTING...
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CONNECTING, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 3 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.connecting,
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.connecting,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    )
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = true,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                    )
                ),
                this
            )
        }

    @Test
    fun `stopConnection - when connection is in CONNECTED state and, after close called, changes to CLOSED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CONNECTED...
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CONNECTED, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 3 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.connected,
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.connected,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    )
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = true,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                    )
                ),
                this
            )
        }

    @Test
    fun `stopConnection - when connection is in DISCONNECTED state and, after close called, changes to CLOSED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns DISCONNECTED...
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is DISCONNECTED, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 3 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.disconnected,
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.disconnected,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    )
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = true,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                    )
                ),
                this
            )
        }

    @Test
    fun `stopConnection - when connection is in SUSPENDED state and, after close called, changes to CLOSED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns SUSPENDED...
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is SUSPENDED, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 3 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.suspended,
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.suspended,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    )
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = true,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                    )
                ),
                this
            )
        }

    @Test
    fun `stopConnection - when connection is in CLOSING state and, after close called, changes to CLOSED`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CLOSING...
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous` is CLOSING, `current` is CLOSED, `retryIn` is (arbitrarily-chosen) 0 and `reason` is (arbitrarily-chosen) null...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 3 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and removes a listener from the connection using `off`...
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.closing,
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange(
                        previous = ConnectionState.closing,
                        current = ConnectionState.closed,
                        retryIn = 0,
                        reason = null
                    )
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = true,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                    )
                ),
                this
            )
        }

    @Test
    fun `stopConnection - when connection is in CLOSED state`() = runTest {
        /* Given...
         *
         * ...that the connection’s `state` property returns CLOSED...
         *
         * When...
         *
         * ...`stopConnection` is called on the object under test...
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it fetches the connection’s state once...
         * ...and the call to `stopConnection` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.StopConnection.test(
            DefaultAblyTestScenarios.StopConnection.GivenConfig(
                initialConnectionState = ConnectionState.closed,
                closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour,
            ),
            DefaultAblyTestScenarios.StopConnection.ThenConfig(
                numberOfConnectionStateFetchesToVerify = 1,
                verifyConnectionOn = false,
                verifyClose = false,
                verifyConnectionOff = false,
                resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `stopConnection - when connection is in FAILED state`() = runTest {
        /* Given...
         *
         * ...that the connection’s `state` property returns FAILED...
         *
         * When...
         *
         * ...`stopConnection` is called on the object under test...
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it fetches the connection’s state 2 times...
         * ...and the call to `stopConnection` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.StopConnection.test(
            DefaultAblyTestScenarios.StopConnection.GivenConfig(
                initialConnectionState = ConnectionState.failed,
                closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour
            ),
            DefaultAblyTestScenarios.StopConnection.ThenConfig(
                numberOfConnectionStateFetchesToVerify = 2,
                verifyConnectionOn = false,
                verifyClose = false,
                verifyConnectionOff = false,
                resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `stopConnection - when, after close is called, no connection state change occurs`() =
        runTest {
            /* Given...
             *
             * ...that the connection’s `state` property returns CONNECTED (arbitrarily chosen)...
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state 2 times...
             * ...and adds a listener to the connection using `on`...
             * ...and tells the Realtime instance to close...
             * ...and the call to `stopConnection` (on the object under test) does not complete (see “Documenting the absence of built-in timeout” above).
             */

            DefaultAblyTestScenarios.StopConnection.test(
                DefaultAblyTestScenarios.StopConnection.GivenConfig(
                    initialConnectionState = ConnectionState.connected, // arbitrarily chosen
                    closeBehaviour = DefaultAblyTestScenarios.GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour
                ),
                DefaultAblyTestScenarios.StopConnection.ThenConfig(
                    numberOfConnectionStateFetchesToVerify = 3,
                    verifyConnectionOn = true,
                    verifyClose = true,
                    verifyConnectionOff = false,
                    resultOfStopConnectionCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                        expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithException(
                            exceptionClass = TimeoutCancellationException::class.java
                        )
                    )
                ),
                this
            )
        }

    /*
    Observations from writing black-box tests for `sendRawLocation`:

    - When given a channel in certain states, it seems to fetch the channel’s state more than once. I have not tested what happens if a different state is returned on the second call.
     */

    @Test
    fun `sendRawLocation - when publish succeeds`() = runTest {
        /* Given...
         *
         * ...that the Channels instance’s `containsKey` method returns true...
         * ...and that the Channels instance’s `get` method (the overload that does not accept a ChannelOptions object) returns a channel...
         * ...which, when told to publish a message, does so successfully...
         *
         * When...
         *
         * ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...it calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to publish a message whose `name` property is "raw"...
         * ...and the call to `sendRawLocation` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.SendRawLocation.test(
            DefaultAblyTestScenarios.SendRawLocation.GivenConfig(
                channelsContainsKey = true,
                mockChannelsGet = true,
                publishBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success
            ),
            DefaultAblyTestScenarios.SendRawLocation.ThenConfig(
                verifyChannelsGet = true,
                verifyPublish = true,
                resultOfSendRawLocationCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `sendRawLocation - when publish fails`() = runTest {
        /* Given...
         *
         * ...that the Channels instance’s `containsKey` method returns true...
         * ...and that the Channels instance’s `get` method (the overload that does not accept a ChannelOptions object) returns a channel...
         * ...which, when told to publish a message, fails to do so with arbitrarily-chosen error `publishError`...
         *
         * When...
         *
         * ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to publish a message whose `name` property is "raw"...
         * ...and the call to `sendRawLocation` (on the object under test) fails with a ConnectionException whose `code` and `message` are equal to those of `publishError`.
         */

        val publishError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        DefaultAblyTestScenarios.SendRawLocation.test(
            DefaultAblyTestScenarios.SendRawLocation.GivenConfig(
                channelsContainsKey = true,
                mockChannelsGet = true,
                publishBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                    publishError
                )
            ),
            DefaultAblyTestScenarios.SendRawLocation.ThenConfig(
                verifyChannelsGet = true,
                verifyPublish = true,
                resultOfSendRawLocationCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            publishError.code,
                            0,
                            publishError.message,
                            null,
                            null
                        )
                    )
                )
            ),
            this
        )
    }

    @Test
    fun `sendRawLocation - when publish doesn't complete`() = runTest {
        /* Given...
         *
         * ...that the Channels instance’s `containsKey` method returns true...
         * ...and that the Channels instance’s `get` method (the overload that does not accept a ChannelOptions object) returns a channel...
         * ...which, when told to publish a message, never finishes doing so...
         *
         * When...
         *
         * ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
         * ...and tells the channel to publish a message whose `name` property is "raw"...
         * ...and the call to `sendRawLocation` (on the object under test) does not complete (see “Documenting the absence of built-in timeout” above).
         */

        DefaultAblyTestScenarios.SendRawLocation.test(
            DefaultAblyTestScenarios.SendRawLocation.GivenConfig(
                channelsContainsKey = true,
                mockChannelsGet = true,
                publishBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete
            ),
            DefaultAblyTestScenarios.SendRawLocation.ThenConfig(
                verifyChannelsGet = true,
                verifyPublish = true,
                resultOfSendRawLocationCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.DoesNotTerminate(
                    timeoutInMilliseconds = noTimeoutDemonstrationWaitingTimeInMilliseconds
                )
            ),
            this
        )
    }

    @Test
    fun `sendRawLocation - when channel doesn't exist`() = runTest {
        /* Given...
         *
         * ...that the Channels instance’s `containsKey` method returns false...
         *
         * When...
         *
         * ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         * ...and the call to `sendRawLocation` (on the object under test) succeeds.
         */

        DefaultAblyTestScenarios.SendRawLocation.test(
            DefaultAblyTestScenarios.SendRawLocation.GivenConfig(
                channelsContainsKey = false,
                mockChannelsGet = false,
                publishBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked
            ),
            DefaultAblyTestScenarios.SendRawLocation.ThenConfig(
                verifyChannelsGet = false,
                verifyPublish = false,
                resultOfSendRawLocationCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedAsyncResult.Terminates(
                    expectedResult = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            ),
            this
        )
    }

    @Test
    fun `close - behaviour when all presence leave calls succeed`() = runTest {
        // Given...
        // ...that the Realtime instance has 3 channels...
        val testEnvironment =
            DefaultAblyTestEnvironment.create(numberOfTrackables = 3, coroutineScope = this)

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
        // ... we call `close` on the object under test,
        testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))

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
    fun `close - behaviour when some presence leave calls succeed and others fail`() = runTest {
        // Given...
        // ...that the Realtime instance has 3 channels...
        val testEnvironment =
            DefaultAblyTestEnvironment.create(numberOfTrackables = 3, coroutineScope = this)

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
        // ... we call `close` on the object under test,
        testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))

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
    fun `close - behaviour when wrapped in a withTimeout block, when the timeout elapses during the presence leave operation`() =
        runTest {
            // Given...
            // ...that the Realtime instance has 1 channel...
            val testEnvironment =
                DefaultAblyTestEnvironment.create(numberOfTrackables = 1, coroutineScope = this)
            val configuredChannel = testEnvironment.configuredChannels[0]

            configuredChannel.mockName()
            // ...which is in the ATTACHED state...
            configuredChannel.mockState(ChannelState.attached)

            // ...and which, when asked to leave presence, never finishes doing so...
            configuredChannel.mockNonCompletingPresenceLeave()

            testEnvironment.mockChannelsEntrySet()

            // When...
            var caughtTimeoutCancellationException = false
            try {
                // ... we call `close` on the object under test within a withTimeout block, whose timeMillis is chosen arbitrarily but hopefully large enough so that internally, the call to `close` gets as far as telling the channel to leave presence...
                withTimeout(1000) {
                    testEnvironment.objectUnderTest.close(PresenceData("" /* arbitrary value */))
                }
            } catch (_: TimeoutCancellationException) {
                caughtTimeoutCancellationException = true
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
