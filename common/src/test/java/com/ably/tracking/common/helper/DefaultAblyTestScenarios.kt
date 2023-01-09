package com.ably.tracking.common.helper

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.AblySdkChannelStateListener
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.AblySdkRealtime
import io.ably.lib.realtime.ChannelState
import io.ably.lib.types.ErrorInfo
import io.mockk.confirmVerified
import io.mockk.verifyOrder
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
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
            object DoesNotComplete : CompletionListenerMockBehaviour()
        }

        /**
         * Describes how a test case should interact with the [AblySdkChannelStateListener] instances added to a channel using [AblySdkRealtime.Channel.on]. Individual test cases should document how they interpret the values this class can take.
         */
        sealed class ChannelStateChangeBehaviour() {
            object NoBehaviour : ChannelStateChangeBehaviour()
            class EmitStateChange(val current: ChannelState) : ChannelStateChangeBehaviour()
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

        /**
         * Describes the expected result of an asynchronous operation which returns a [Result] instance to communicate its result.
         */
        sealed class ExpectedAsyncResult {
            /**
             * The operation terminates and its result is described by [expectedResult].
             */
            class Terminates(val expectedResult: ExpectedResult) : ExpectedAsyncResult()

            /**
             * The operation does not terminate within [timeoutInMilliseconds] milliseconds.
             */
            class DoesNotTerminate(val timeoutInMilliseconds: Long) : ExpectedAsyncResult()

            /**
             * Given [result], which represents the result of an asynchronous operation `op`, this implements the following steps of the "Then..." part of a parameterised test case:
             *
             * ```text
             * when ${this} is Terminates and ${this.expectedResult} is Success {
             * ...and ${op} succeeds.
             * }
             *
             * when ${this} is Terminates and ${this.expectedResult} is FailureWithConnectionException {
             * ...and ${op} fails with a ConnectionException whose errorInformation is equal to ${this.errorInformation}.
             * }
             *
             * when ${this} is DoesNotTerminate {
             * ...and ${op} does not complete within ${this.timeoutInMilliseconds} milliseconds.
             * }
             * ```
             *
             * @param result `null` if the execution of `op` lasted longer than some timeout described by `this`, else the result of `op`’s execution. For example, a value returned by executing `op` using [executeForVerifying], with `this` as the `expectedAsyncResult` argument.
             */
            fun <T> verify(result: Result<T>?) {
                when (this) {
                    is Terminates -> {
                        /* when ${this} is Terminates and ${this.expectedResult} is Success {
                         * ...and ${op} succeeds.
                         * }
                         *
                         * when ${this} is Terminates and ${this.expectedResult} is FailureWithConnectionException {
                         * ...and ${op} fails with a ConnectionException whose errorInformation is equal to ${this.errorInformation}.
                         * }
                         */
                        Assert.assertNotNull(result)
                        if (result != null) {
                            expectedResult.verify(result)
                        }
                    }

                    is DoesNotTerminate -> {
                        /* when ${this} is DoesNotTerminate {
                         * ...and ${op} does not complete within ${this.timeoutInMilliseconds} milliseconds.
                         * }
                         */
                        Assert.assertNull(result)
                    }
                }
            }
        }
    }

    /**
     * This exception is thrown when a test scenario is executed with an invalid configuration due to programmer error.
     */
    class InvalidTestConfigurationException(message: String) : Exception(message)

    companion object {
        /**
         * Executes asynchronous operation [operation] in an environment suitable for later verifying whether its result was as described by [expectedAsyncResult].
         *
         * @param expectedAsyncResult The expected result of [operation].
         * @param operation The operation to execute, whose result the caller intends to subsequently verify [expectedAsyncResult] as describing.
         * @return `null` if the execution of [operation] lasted longer than some timeout described by [expectedAsyncResult], else the result of [operation]’s execution. You can later pass this value to [ThenTypes.ExpectedAsyncResult.verify].
         */
        suspend fun <T> executeForVerifying(
            expectedAsyncResult: ThenTypes.ExpectedAsyncResult,
            operation: suspend () -> T
        ): T? {
            return when (expectedAsyncResult) {
                is ThenTypes.ExpectedAsyncResult.Terminates -> {
                    operation()
                }
                is ThenTypes.ExpectedAsyncResult.DoesNotTerminate -> {
                    try {
                        withTimeout(timeMillis = expectedAsyncResult.timeoutInMilliseconds) {
                            /* This usage of runInterruptible is intended to ensure that even if `operation` is not cancellable — that is, even if it does not cooperate with cancellation (https://kotlinlang.org/docs/cancellation-and-timeouts.html#cancellation-is-cooperative) — the withTimeout method call will return after the timeout elapses. We have some methods in DefaultAbly that are not cancellable – see https://github.com/ably/ably-asset-tracking-android/issues/908.
                             *
                             * Perhaps there’s a better way to do this — to create a coroutine that’s cancellable even if it calls a non-cancellable one — that doesn’t involve making a trip from coroutines land to synchronous land and back again. I’m not familiar enough with the coroutines APIs to know.
                             */

                            runInterruptible {
                                runBlocking {
                                    operation()
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        null
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
            val channelsContainsKey: Boolean,
            val channelsGetOverload: DefaultAblyTestEnvironment.ChannelsGetOverload,
            val channelState: ChannelState,
            val channelAttachBehaviour: GivenTypes.CompletionListenerMockBehaviour,
            val presenceEnterBehaviour: GivenTypes.CompletionListenerMockBehaviour
        )

        /**
         * This class provides properties for configuring the "Then..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class ThenConfig(
            val overloadOfChannelsGetToVerify: DefaultAblyTestEnvironment.ChannelsGetOverload,
            val numberOfChannelStateFetchesToVerify: Int,
            val verifyPresenceEnter: Boolean,
            val verifyChannelAttach: Boolean,
            val verifyChannelRelease: Boolean,
            val resultOfConnectCallOnObjectUnderTest: ThenTypes.ExpectedAsyncResult
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
             * when ${givenConfig.channelAttachBehaviour} is Success {
             * ...which, when told to attach, does so successfully...
             * }
             *
             * when ${givenConfig.channelAttachBehaviour} is Failure {
             * ...which, when told to attach, fails to do so with error ${givenConfig.channelAttachBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.channelAttachBehaviour} is DoesNotComplete {
             * ...which, when told to attach, never finishes doing so...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is Success {
             * ...[and] which, when told to enter presence, does so successfully...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is Failure {
             * ...[and] which, when told to enter presence, fails to do so with error ${givenConfig.presenceEnterBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is DoesNotComplete {
             * ...[and] which, when told to enter presence, never finishes doing so...
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
             * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfConnectCallOnObjectUnderTest.expectedResult} is Success {
             * ...and the call to `connect` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfConnectCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
             * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfConnectCallOnObjectUnderTest.errorInformation}.
             * }
             *
             * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is DoesNotTerminate {
             * ...and the call to `connect` (on the object under test) does not complete within ${thenConfig.resultOfConnectCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
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

                testEnvironment.stubRelease(configuredChannel)

                when (val givenChannelAttachBehaviour = givenConfig.channelAttachBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.channelAttachBehaviour} is Success {
                     * ...which, when told to attach, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulAttach()
                    }
                    /* when ${givenConfig.channelAttachBehaviour} is Failure {
                     * ...which, when told to attach, fails to do so with error ${givenConfig.channelAttachBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedAttach(givenChannelAttachBehaviour.errorInfo)
                    }
                    /* when ${givenConfig.channelAttachBehaviour} is DoesNotComplete {
                     * ...which, when told to attach, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingAttach()
                    }
                }

                when (val givenPresenceEnterBehaviour = givenConfig.presenceEnterBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.presenceEnterBehaviour} is Success {
                     * ...[and] which, when told to enter presence, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulPresenceEnter()
                    }
                    /* when ${givenConfig.presenceEnterBehaviour} is Failure {
                     * ...[and] which, when told to enter presence, fails to do so with error ${givenConfig.presenceEnterBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedPresenceEnter(givenPresenceEnterBehaviour.errorInfo)
                    }
                    /* when ${givenConfig.presenceEnterBehaviour} is DoesNotComplete {
                     * ...[and] which, when told to enter presence, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingPresenceEnter()
                    }
                }

                // When...

                val result = executeForVerifying(thenConfig.resultOfConnectCallOnObjectUnderTest) {
                    // ...we call `connect` on the object under test,
                    testEnvironment.objectUnderTest.connect(
                        configuredChannel.trackableId,
                        PresenceData("")
                    )
                }

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

                    if (thenConfig.verifyChannelRelease) {
                        /* if ${thenConfig.verifyChannelRelease} {
                         * ...and releases the channel...
                         * }
                         */
                        testEnvironment.channelsMock.release(configuredChannel.channelName)
                    }
                }

                /* when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfConnectCallOnObjectUnderTest.expectedResult} is Success {
                 * ...and the call to `connect` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfConnectCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
                 * ...and the call to `connect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfConnectCallOnObjectUnderTest.errorInformation}.
                 * }
                 *
                 * when ${thenConfig.resultOfConnectCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `connect` (on the object under test) does not complete within ${thenConfig.resultOfConnectCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
                 * }
                 */
                thenConfig.resultOfConnectCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }

    /**
     * Provides test scenarios for [DefaultAbly.updatePresenceData]. See the [Companion.test] method.
     */
    class UpdatePresenceData {
        /**
         * This class provides properties for configuring the "Given..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class GivenConfig(
            val channelsContainsKey: Boolean,
            val mockChannelsGet: Boolean,
            /**
             * This must be `null` if and only if [mockChannelsGet] is `false`.
             */
            val initialChannelState: ChannelState?,
            /**
             * If [mockChannelsGet] is `false` then this must be [GivenTypes.ChannelStateChangeBehaviour.NoBehaviour].
             */
            val channelStateChangeBehaviour: GivenTypes.ChannelStateChangeBehaviour,
            /**
             * If [mockChannelsGet] is `false` then this must be [GivenTypes.CompletionListenerMockBehaviour.NotMocked].
             */
            val presenceUpdateBehaviour: GivenTypes.CompletionListenerMockBehaviour
        ) {
            /**
             * Checks that this object represents a valid test configuration.
             *
             * @throws InvalidTestConfigurationException If this object does not represent a valid test configuration.
             */
            fun validate() {
                if (mockChannelsGet) {
                    if (initialChannelState == null) {
                        throw InvalidTestConfigurationException("initialChannelState must be non-null when mockChannelsGet is true")
                    }
                } else {
                    if (initialChannelState != null) {
                        throw InvalidTestConfigurationException("initialChannelState must be null when mockChannelsGet is false")
                    }
                    if (channelStateChangeBehaviour !is GivenTypes.ChannelStateChangeBehaviour.NoBehaviour) {
                        throw InvalidTestConfigurationException("channelStateChangeBehaviour must be NoBehaviour when mockChannelsGet is false")
                    }
                    if (presenceUpdateBehaviour !is GivenTypes.CompletionListenerMockBehaviour.NotMocked) {
                        throw InvalidTestConfigurationException("presenceUpdateBehaviour must be NotMocked when mockChannelsGet is false")
                    }
                }
            }
        }

        /**
         * This class provides properties for configuring the "Then..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class ThenConfig(
            val verifyChannelsGet: Boolean,
            /**
             * If [GivenConfig.mockChannelsGet] is `false` then this must be zero.
             */
            val numberOfChannelStateFetchesToVerify: Int,
            /**
             * If [GivenConfig.mockChannelsGet] is `false` then this must be `false`.
             */
            val verifyChannelOn: Boolean,
            /**
             * If [GivenConfig.channelStateChangeBehaviour] is not [GivenTypes.ChannelStateChangeBehaviour.EmitStateChange] then this must be `false`.
             */
            val verifyChannelStateChangeCurrent: Boolean,
            /**
             * If [GivenConfig.mockChannelsGet] is `false` then this must be `false`.
             */
            val verifyChannelOff: Boolean,
            /**
             * If [GivenConfig.mockChannelsGet] is `false` then this must be `false`.
             */
            val verifyPresenceUpdate: Boolean,
            val resultOfUpdatePresenceCallOnObjectUnderTest: ThenTypes.ExpectedAsyncResult
        ) {
            /**
             * Checks that this object represents a valid test configuration to be used with [givenConfig].
             *
             * @param givenConfig The configuration that `this` is intended to be used with.
             * @throws InvalidTestConfigurationException If this object does not represent a valid test configuration.
             */
            fun validate(givenConfig: GivenConfig) {
                if (!givenConfig.mockChannelsGet) {
                    if (numberOfChannelStateFetchesToVerify != 0) {
                        throw InvalidTestConfigurationException("numberOfChannelStateFetchesToVerify must be zero when mockChannelsGet is false")
                    }
                    if (verifyChannelOn) {
                        throw InvalidTestConfigurationException("verifyChannelOn must be false when mockChannelsGet is false")
                    }
                    if (verifyChannelOff) {
                        throw InvalidTestConfigurationException("verifyChannelOn must be false when mockChannelsGet is false")
                    }
                    if (verifyPresenceUpdate) {
                        throw InvalidTestConfigurationException("verifyPresenceUpdate must be false when mockChannelsGet is false")
                    }
                }
                if (givenConfig.channelStateChangeBehaviour !is GivenTypes.ChannelStateChangeBehaviour.EmitStateChange) {
                    if (verifyChannelStateChangeCurrent) {
                        throw InvalidTestConfigurationException("verifyChannelStateChangeCurrent must be false when channelStateChangeBehaviour is not EmitStateChange")
                    }
                }
            }
        }

        companion object {
            /**
             * Implements the following parameterised test case for [DefaultAbly.updatePresenceData]:
             *
             * ```text
             * Given...
             *
             * ...that calling `containsKey` on the Channels instance returns ${givenConfig.channelsContainsKey}...
             *
             * if ${givenConfig.mockChannelsGet} {
             * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel in state ${givenConfig.initialChannelState}...
             * }
             *
             * when ${givenConfig.channelStateChangeBehaviour} is EmitStateChange {
             * ...which, when its `on` method is called, immediately calls the received listener with a channel state change whose `current` property is ${givenConfig.channelStateChangeBehaviour.current}...
             * }
             *
             * when ${givenConfig.presenceUpdateBehaviour} is Success {
             * ...[and] which, when told to update presence data, does so successfully...
             * }
             *
             * when ${givenConfig.presenceUpdateBehaviour} is Failure {
             * ...[and] which, when told to update presence data, fails to do so with error ${givenConfig.presenceUpdateBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.presenceUpdateBehaviour} is DoesNotComplete {
             * ...[and] which, when told to update presence data, never finishes doing so...
             * }
             *
             *
             * When...
             *
             * ...we call `updatePresenceData` on the object under test,
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it calls `containsKey` on the Channels instance...
             *
             * if ${thenConfig.verifyChannelsGet} {
             * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
             * }
             *
             * ...and checks the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
             *
             * if ${thenConfig.verifyChannelOn} {
             * ...and calls `on` on the channel...
             * }
             *
             * if ${thenConfig.verifyChannelStateChangeCurrent}
             * ...and checks the state change’s `current` property...
             * }
             *
             * if ${thenConfig.verifyChannelOff} {
             * ...and calls `off` on the channel...
             * }
             *
             * if ${thenConfig.verifyPresenceUpdate} {
             * ...and tells the channel to update presence data...
             * }
             *
             * when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.expectedResult} is Success {
             * ...and the call to `updatePresenceData` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
             * ...and the call to `updatePresenceData` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.errorInformation}.
             * }
             *
             * when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is DoesNotTerminate {
             * ...and the call to `updatePresenceData` (on the object under test) does not complete within ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
             * }
             * ```
             */
            suspend fun test(givenConfig: GivenConfig, thenConfig: ThenConfig) {
                givenConfig.validate()
                thenConfig.validate(givenConfig)

                // Given...

                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
                val configuredChannel = testEnvironment.configuredChannels[0]

                // ...that calling `containsKey` on the Channels instance returns ${givenConfig.channelsContainsKey}...
                testEnvironment.mockChannelsContainsKey(
                    configuredChannel.channelName,
                    givenConfig.channelsContainsKey
                )

                if (givenConfig.mockChannelsGet) {
                    /* if ${givenConfig.mockChannelsGet} {
                     * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel in state ${givenConfig.initialChannelState}...
                     * }
                     */
                    testEnvironment.mockChannelsGet(DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS)
                    configuredChannel.mockState(givenConfig.initialChannelState!!)
                }

                val channelStateChangeMock: AblySdkChannelStateListener.ChannelStateChange?
                when (
                    val givenChannelStateChangeBehaviour =
                        givenConfig.channelStateChangeBehaviour
                ) {
                    is GivenTypes.ChannelStateChangeBehaviour.NoBehaviour -> {
                        configuredChannel.stubOn()
                        channelStateChangeMock = null
                    }
                    is GivenTypes.ChannelStateChangeBehaviour.EmitStateChange -> {
                        /* when ${givenConfig.channelStateChangeBehaviour} is EmitStateChange {
                         * ...which, when its `on` method is called, immediately calls the received listener with a channel state change whose `current` property is ${givenConfig.channelStateChangeBehaviour.current}...
                         * }
                         */
                        channelStateChangeMock =
                            configuredChannel.mockOnToEmitStateChange(current = givenChannelStateChangeBehaviour.current)
                        configuredChannel.stubOff()
                    }
                }

                when (val givenPresenceUpdateBehaviour = givenConfig.presenceUpdateBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.presenceUpdateBehaviour} is Success {
                     * ...[and] which, when told to update presence data, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulPresenceUpdate()
                    }
                    /* when ${givenConfig.presenceUpdateBehaviour} is Failure {
                     * ...[and] which, when told to update presence data, fails to do so with error ${givenConfig.presenceUpdateBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedPresenceUpdate(givenPresenceUpdateBehaviour.errorInfo)
                    }
                    /* when ${givenConfig.presenceUpdateBehaviour} is DoesNotComplete {
                     * ...[and] which, when told to update presence data, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingPresenceUpdate()
                    }
                }

                // When...

                val result = executeForVerifying(thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest) {
                    // ...we call `updatePresenceData` on the object under test,
                    testEnvironment.objectUnderTest.updatePresenceData(
                        configuredChannel.trackableId,
                        PresenceData("") /* arbitrarily chosen */
                    )
                }

                // Then...
                // ...in the following order, precisely the following things happen...
                verifyOrder {
                    // ...it calls `containsKey` on the Channels instance...
                    testEnvironment.channelsMock.containsKey(configuredChannel.channelName)

                    if (thenConfig.verifyChannelsGet) {
                        /* if ${thenConfig.verifyChannelsGet} {
                         * ...and calls `get` (the overload that does not accept a ChannelOptions object) on the Channels instance...
                         * }
                         */
                        testEnvironment.channelsMock.get(configuredChannel.channelName)
                    }

                    repeat(thenConfig.numberOfChannelStateFetchesToVerify) {
                        // ...and checks the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
                        configuredChannel.channelMock.state
                    }

                    if (thenConfig.verifyChannelOn) {
                        /* if ${thenConfig.verifyChannelOn} {
                         * ...and calls `on` on the channel...
                         */
                        configuredChannel.channelMock.on(any())
                    }

                    if (thenConfig.verifyChannelStateChangeCurrent) {
                        /* if ${thenConfig.verifyChannelStateChangeCurrent}
                         * ...and checks the state change’s `current` property...
                         * }
                         */
                        channelStateChangeMock!!.current
                    }

                    if (thenConfig.verifyChannelOff) {
                        /* if ${thenConfig.verifyChannelOff} {
                         * ...and calls `off` on the channel...
                         * }
                         */
                        configuredChannel.channelMock.off(any())
                    }

                    if (thenConfig.verifyPresenceUpdate) {
                        /* if ${thenConfig.verifyPresenceUpdate} {
                         * ...and tells the channel to update presence data...
                         * }
                         */
                        configuredChannel.presenceMock.update(any(), any())
                    }
                }

                /* when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.expectedResult} is Success {
                 * ...and the call to `updatePresenceData` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
                 * ...and the call to `updatePresenceData` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.errorInformation}.
                 * }
                 *
                 * when ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `updatePresenceData` (on the object under test) does not complete within ${thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
                 * }
                 */
                thenConfig.resultOfUpdatePresenceCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }
}
