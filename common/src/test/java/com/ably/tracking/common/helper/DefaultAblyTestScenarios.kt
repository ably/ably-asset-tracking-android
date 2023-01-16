package com.ably.tracking.common.helper

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.helper.DefaultAblyTestScenarios.Connect.Companion
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.ErrorInfo
import io.mockk.confirmVerified
import io.mockk.verifyOrder
import org.junit.Assert

class DefaultAblyTestScenarios {
    /**
     * A collection of types which are used in the various `GivenConfig` classes used for configuring the "Given..." part of a parameterised test case.
     */
    class GivenTypes {
        /**
         * Describes how a test case mocks the behaviour of a method which uses a [CompletionListener] to communicate its result. Individual test cases should document how they interpret the values this class can take.
         */
        sealed class CompletionListenerMockBehaviour() {
            object NotMocked : CompletionListenerMockBehaviour()
            object Success : CompletionListenerMockBehaviour()
            class Failure(val errorInfo: ErrorInfo) : CompletionListenerMockBehaviour()
        }
    }

    /**
     * A collection of types which are used in the various `ThenConfig` classes used for configuring the "Then..." part of a parameterised test case.
     */
    class ThenTypes {
        /**
         * Describes the expected result of an operation which returns a [Result] instance to communicate its result.
         */
        sealed class ExpectedResult() {
            /**
             * The operation succeeds.
             */
            object Success : ExpectedResult()

            /**
             * The operation fails with a [ConnectionException] whose [ConnectionException.errorInformation] is equal to [errorInformation].
             */
            class FailureWithConnectionException(val errorInformation: ErrorInformation) :
                ExpectedResult()

            /**
             * Given [result], which represents the result of an operation `op`, this implements the following steps of the "Then..." part of a parameterised test case:
             *
             * ```text
             * when ${this} is Success {
             * ...and ${op} succeeds.
             * }
             *
             * when ${this} is FailureWithConnectionException {
             * ...and ${op} fails with a ConnectionException whose errorInformation is equal to ${this.errorInformation}.
             * }
             * ```
             *
             * @param result The result of the operation `op`.
             */
            fun <T> verify(result: Result<T>) {
                when (this) {
                    is Success -> {
                        /* when ${this} is Success {
                         * ...and ${op} succeeds.
                         * }
                         */
                        Assert.assertTrue(result.isSuccess)
                    }
                    is FailureWithConnectionException -> {
                        /* when ${this} is FailureWithConnectionException {
                         * ...and ${op} fails with a ConnectionException whose errorInformation is equal to ${this.errorInformation}.
                         * }
                         */
                        Assert.assertTrue(result.isFailure)
                        val exception = result.exceptionOrNull()!!
                        Assert.assertTrue(exception is ConnectionException)
                        val connectionException = exception as ConnectionException
                        Assert.assertEquals(
                            this.errorInformation,
                            connectionException.errorInformation
                        )
                    }
                }
            }
        }
    }

    /**
     * Provides test scenarios for [DefaultAbly.connect]. See the [Companion.test] method.
     */
    class Connect {
        /**
         * This class provides properties for configuring the "Given..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class GivenConfig(
            val connectionState: ConnectionState?,
            val channelsContainsKey: Boolean,
            val channelsGetOverload: DefaultAblyTestEnvironment.ChannelsGetOverload,
            val channelState: ChannelState,
            val presenceEnterBehaviour: GivenTypes.CompletionListenerMockBehaviour,
            val channelAttachBehaviour: GivenTypes.CompletionListenerMockBehaviour
        )

        /**
         * This class provides properties for configuring the "Then..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class ThenConfig(
            val overloadOfChannelsGetToVerify: DefaultAblyTestEnvironment.ChannelsGetOverload,
            val numberOfChannelStateFetchesToVerify: Int,
            val verifyPresenceEnter: Boolean,
            val verifyChannelAttach: Boolean,
            val numberOfChannelStateFetchesToVerifyAfterPresence: Int,
            val verifyConnectionStateFetch: Boolean,
            val verifyChannelRelease: Boolean,
            val resultOfConnectCallOnObjectUnderTest: ThenTypes.ExpectedResult
        )

        companion object {
            /**
             * Implements the following parameterised test case for [DefaultAbly.connect]:
             *
             * ```text
             * Given...
             *
             * ...that calling `containsKey` on the Channels instance returns ${givenConfig.channelsContainsKey}...
             * ...and that calling `get` (the overload described by ${givenConfig.channelsGetOverload}) on the Channels instance returns a channel in the ${givenConfig.channelState} state...
             *
             * when ${givenConfig.presenceEnterBehaviour} is Success {
             * ...which, when told to enter presence, does so successfully...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is Failure {
             * ...which, when told to enter presence, fails to do so with error ${givenConfig.presenceEnterBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.channelAttachBehaviour} is Success {
             * ...[and] which, when told to attach, does so successfully...
             * }
             *
             * when ${givenConfig.channelAttachBehaviour} is Failure {
             * ...[and] which, when told to attach, fails to do so with error ${givenConfig.channelAttachBehaviour.errorInfo}...
             * }
             *
             * When...
             *
             * ...we call `connect` on the object under test,
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it calls `containsKey` on the Channels instance...
             * ...and calls `get` (the overload described by ${givenConfig.channelsGetOverload}) on the Channels instance...
             * ...and checks the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
             *
             * if ${thenConfig.verifyChannelAttach} {
             * ...and tells the channel to attach...
             * }
             *
             * if ${thenConfig.verifyPresenceEnter} {
             * ...and tells the channel to enter presence...
             * }
             *
             * if ${thenConfig.verifyChannelRelease} {
             * ...and releases the channel...
             * }
             *
             * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Success {
             * ...and the call to `connect` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is FailureWithConnectionException {
             * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfConnectCallOnObjectUnderTest.errorInfo}.
             * }
             * ```
             *
             * @param givenConfig Parameters for the "Given..." part of the test case.
             * @param thenConfig Parameters for the "Then..." part of the test case.
             */
            suspend fun test(
                givenConfig: GivenConfig,
                thenConfig: ThenConfig
            ) {
                // Given...
                // ...that calling `containsKey` on the Channels instance returns ${givenConfig.channelsContainsKey}...
                // ...and that calling `get` (the overload described by ${givenConfig.channelsGetOverload}) on the Channels instance returns a channel in the ${givenConfig.channelState} state...
                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
                val configuredChannel = testEnvironment.configuredChannels[0]
                testEnvironment.mockChannelsContainsKey(
                    key = configuredChannel.channelName,
                    result = givenConfig.channelsContainsKey
                )
                testEnvironment.mockChannelsGet(givenConfig.channelsGetOverload)
                configuredChannel.mockState(givenConfig.channelState)

                givenConfig.connectionState?.let { connectionState ->
                    testEnvironment.mockConnectionState(connectionState)
                }

                testEnvironment.stubRelease(configuredChannel)

                when (val givenPresenceEnterBehaviour = givenConfig.presenceEnterBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.presenceEnterBehaviour} is Success {
                     * ...which, when told to enter presence, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulPresenceEnter()
                    }
                    /* when ${givenConfig.presenceEnterBehaviour} is Failure {
                     * ...which, when told to enter presence, fails to do so with error ${givenConfig.presenceEnterBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedPresenceEnter(givenPresenceEnterBehaviour.errorInfo)
                    }
                }

                when (val givenChannelAttachBehaviour = givenConfig.channelAttachBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.channelAttachBehaviour} is Success {
                     * ...[and] which, when told to attach, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulAttach()
                    }
                    /* when ${givenConfig.channelAttachBehaviour} is Failure {
                     * ...[and] which, when told to attach, fails to do so with error ${givenConfig.channelAttachBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedAttach(givenChannelAttachBehaviour.errorInfo)
                    }
                }

                // When...

                // ...we call `connect` on the object under test,
                val result = testEnvironment.objectUnderTest.connect(
                    configuredChannel.trackableId,
                    PresenceData("")
                )

                // Then...
                // ...in the following order, precisely the following things happen...
                verifyOrder {
                    // ...it calls `containsKey` on the Channels instance...
                    testEnvironment.channelsMock.containsKey(configuredChannel.channelName)

                    // ...and calls `get` (the overload described by ${thenConfig.overloadOfChannelsGetToVerify}) on the Channels instance...
                    when (thenConfig.overloadOfChannelsGetToVerify) {
                        DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS -> {
                            testEnvironment.channelsMock.get(configuredChannel.channelName)
                        }
                        DefaultAblyTestEnvironment.ChannelsGetOverload.WITH_CHANNEL_OPTIONS -> {
                            testEnvironment.channelsMock.get(configuredChannel.channelName, any())
                        }
                    }

                    // ...and checks the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
                    repeat(thenConfig.numberOfChannelStateFetchesToVerify) {
                        configuredChannel.channelMock.state
                    }

                    if (thenConfig.verifyChannelAttach) {
                        /* if ${thenConfig.verifyChannelAttach} {
                         * ...and tells the channel to attach...
                         * }
                         */
                        configuredChannel.channelMock.attach(any())
                    }

                    if (thenConfig.verifyPresenceEnter) {
                        /* if ${thenConfig.verifyPresenceEnter} {
                         * ...and tells the channel to enter presence...
                         * }
                         */
                        configuredChannel.presenceMock.enter(any(), any())
                    }

                    // ...and checks the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerifyAfterPresence} times...
                    repeat(thenConfig.numberOfChannelStateFetchesToVerifyAfterPresence) {
                        configuredChannel.channelMock.state
                    }

                    if (thenConfig.verifyConnectionStateFetch) {
                        /* if ${thenConfig.verifyConnectionStateFetch} {
                         * ...and checks the connection's state...
                         * }
                         */
                        testEnvironment.connectionMock.state
                    }

                    if (thenConfig.verifyChannelRelease) {
                        /* if ${thenConfig.verifyChannelRelease} {
                         * ...and releases the channel...
                         * }
                         */
                        testEnvironment.channelsMock.release(configuredChannel.channelName)
                    }
                }

                /* when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Success {
                 * ...and the call to `connect` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is FailureWithConnectionException {
                 * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfConnectCallOnObjectUnderTest.errorInfo}.
                 * }
                 */
                thenConfig.resultOfConnectCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }
}
