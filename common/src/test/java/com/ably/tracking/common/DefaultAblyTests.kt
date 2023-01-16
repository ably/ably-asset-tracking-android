package com.ably.tracking.common

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.helper.DefaultAblyTestEnvironment
import com.ably.tracking.common.helper.DefaultAblyTestScenarios
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
    /*
    Observations from writing black-box tests for `connect`:

    - When given a channel in certain states, it seems to fetch the channel’s state more than once. I have not tested what happens if a different state is returned on the second call.
     */

    @Test
    fun `connect - when channel fetched is in INITIALIZED state`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the INITIALIZED state...
         * ...which, when told to enter presence, does so successfully,
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.initialized,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in ATTACHED state`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the ATTACHED state...
         * ...which, when told to enter presence, does so successfully,
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.attached,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in ATTACHING state`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the ATTACHING state...
         * ...which, when told to enter presence, does so successfully...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.attaching,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in DETACHING state`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the DETACHING state...
         * ...which, when told to enter presence, does so successfully...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.detaching,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - connection is in a connected state and when presence enter fails`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` on the connection state returns (arbitrarily-chosen) failed state ...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the (arbitrarily-chosen) INITIALIZED state...
         * ...which, when told to enter presence, fails to do so with an arbitrarily-chosen error `presenceError`...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `presenceError`.
         */

        val presenceError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = ConnectionState.connected,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS, /* arbitrarily chosen */
                    channelState = ChannelState.initialized,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                        presenceError
                    ),
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 1,
                    verifyConnectionStateFetch = true,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - connection is in a failed state and when presence enter fails`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` on the connection state returns (arbitrarily-chosen) failed state ...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the (arbitrarily-chosen) INITIALIZED state...
         * ...which, when told to enter presence, fails to do so with an arbitrarily-chosen error `presenceError`...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `presenceError`.
         */

        val presenceError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = ConnectionState.failed,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS, /* arbitrarily chosen */
                    channelState = ChannelState.initialized,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                        presenceError
                    ),
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 1,
                    verifyConnectionStateFetch = true,
                    verifyChannelRelease = true,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(
                            presenceError.code,
                            0,
                            presenceError.message,
                            null,
                            null
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in FAILED state and attach succeeds`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the FAILED state...
         * ...which, when told to enter presence, does so successfully...
         * ...and which, when told to attach, does so successfully...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to attach...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.failed,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = true,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in FAILED state and attach fails`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the FAILED state...
         * ...which, when told to attach, fails to do so with an arbitrarily-chosen error `attachError`...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to attach...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `attachError`.
         */

        val attachError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.failed,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                        attachError
                    ),
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = false,
                    verifyChannelAttach = true,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 1,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = true,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(attachError.code, 0, attachError.message, null, null)
                    ),
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in DETACHED state and attach succeeds`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the DETACHED state...
         * ...which, when told to enter presence, does so successfully...
         * ...and which, when told to attach, does so successfully...
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
         * ...and checks the channel’s state once...
         * ...and tells the channel to attach...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.detached,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 1,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = true,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when  connection is in a connected state, channel fetched is in DETACHED state, and attach fails`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` on the connection state returns (arbitrarily-chosen) failed state ...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the DETACHED state...
         * ... which, when told to attach, fails to do so with an arbitrarily-chosen error...
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
         * ...and checks the channel’s state once...
         * ...and tells the channel to attach...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `attachError`.
         */

        val attachError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = ConnectionState.connected,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.detached,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                        attachError
                    ),
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 1,
                    verifyPresenceEnter = false,
                    verifyChannelAttach = true,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 1,
                    verifyConnectionStateFetch = true,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when  connection is in a failed state, channel fetched is in DETACHED state, and attach fails`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` on the connection state returns (arbitrarily-chosen) failed state ...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the DETACHED state...
         * ... which, when told to attach, fails to do so with an arbitrarily-chosen error...
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
         * ...and checks the channel’s state once...
         * ...and tells the channel to attach...
         * ...and releases the channel...
         * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation has the same `code` and `message` as `attachError`.
         */

        val attachError = ErrorInfo(
            "example of an error message", /* arbitrarily chosen */
            123 /* arbitrarily chosen */
        )

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = ConnectionState.failed,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.detached,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Failure(
                        attachError
                    ),
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 1,
                    verifyPresenceEnter = false,
                    verifyChannelAttach = true,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 1,
                    verifyConnectionStateFetch = true,
                    verifyChannelRelease = true,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.FailureWithConnectionException(
                        ErrorInformation(attachError.code, 0, attachError.message, null, null)
                    )
                )
            )
        }
    }

    @Test
    fun `connect - when channel fetched is in SUSPENDED state`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns false...
         * ...and that calling `get` (the overload that accepts a ChannelOptions object) on the Channels instance returns a channel in the SUSPENDED state...
         * ...which, when told to enter presence, does so successfully...
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
         * ...and checks the channel’s state 2 times...
         * ...and tells the channel to enter presence...
         * ...and the call to `connect` (on the object under test) succeeds.
         */

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = false,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    channelState = ChannelState.suspended,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 2,
                    verifyPresenceEnter = true,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
        }
    }

    @Test
    fun `connect - when channel already exists`() {
        /* Given...
         *
         * ...that calling `containsKey` on the Channels instance returns true...
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel in the (arbitrarily-chosen) INITIALIZED state...
         * ...which, when told to enter presence, does so successfully,
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

        runBlocking {
            DefaultAblyTestScenarios.Connect.test(
                DefaultAblyTestScenarios.Connect.GivenConfig(
                    connectionState = null,
                    channelsContainsKey = true,
                    channelsGetOverload = DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS, /* arbitrarily chosen */
                    channelState = ChannelState.initialized,
                    presenceEnterBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.Success,
                    channelAttachBehaviour = DefaultAblyTestScenarios.GivenTypes.CompletionListenerMockBehaviour.NotMocked,
                ),
                DefaultAblyTestScenarios.Connect.ThenConfig(
                    overloadOfChannelsGetToVerify = DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS,
                    numberOfChannelStateFetchesToVerify = 0,
                    verifyPresenceEnter = false,
                    verifyChannelAttach = false,
                    numberOfChannelStateFetchesToVerifyAfterPresence = 0,
                    verifyConnectionStateFetch = false,
                    verifyChannelRelease = false,
                    resultOfConnectCallOnObjectUnderTest = DefaultAblyTestScenarios.ThenTypes.ExpectedResult.Success
                )
            )
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
