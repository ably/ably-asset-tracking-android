package com.ably.tracking.common.helper

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.AblySdkChannelStateListener
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.AblySdkRealtime
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import io.ably.lib.types.ErrorInfo
import io.mockk.confirmVerified
import io.mockk.verifyOrder
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

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

        /**
         * Describes how a test case should interact with the [ConnectionStateListener] instances added to a connection using [AblySdkRealtime.Connection.on]. Individual test cases should document how they interpret the values this class can take.
         */
        sealed class ConnectionStateChangeBehaviour() {
            object NoBehaviour : ConnectionStateChangeBehaviour()
            class EmitStateChange(
                val previous: ConnectionState,
                val current: ConnectionState,
                val retryIn: Long,
                val reason: ErrorInfo?
            ) : ConnectionStateChangeBehaviour()
        }

        /**
         * Describes how a test case mocks the return value of an [AblySdkRealtime.Connection] object’s [AblySdkRealtime.Connection.reason] property. Individual test cases should document how they interpret the values this class can take.
         */
        sealed class ConnectionReasonMockBehaviour() {
            object NotMocked : ConnectionReasonMockBehaviour()
            class Mocked(val reason: ErrorInfo?) : ConnectionReasonMockBehaviour()
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
             * when ${givenConfig.presenceEnterBehaviour} is Success {
             * ...which, when told to enter presence, does so successfully...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is Failure {
             * ...which, when told to enter presence, fails to do so with error ${givenConfig.presenceEnterBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.presenceEnterBehaviour} is DoesNotComplete {
             * ...which, when told to enter presence, never finishes doing so...
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
             * when ${givenConfig.channelAttachBehaviour} is DoesNotComplete {
             * ...[and] which, when told to attach, never finishes doing so...
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
                    /* when ${givenConfig.presenceEnterBehaviour} is DoesNotComplete {
                     * ...which, when told to enter presence, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingPresenceEnter()
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
                    /* when ${givenConfig.channelAttachBehaviour} is DoesNotComplete {
                     * ...[and] which, when told to attach, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingAttach()
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

    /**
     * Provides test scenarios for [DefaultAbly.disconnect]. See the [Companion.test] method.
     */
    class Disconnect {
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
            val presenceLeaveBehaviour: GivenTypes.CompletionListenerMockBehaviour
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
                    if (presenceLeaveBehaviour !is GivenTypes.CompletionListenerMockBehaviour.NotMocked) {
                        throw InvalidTestConfigurationException("presenceLeaveBehaviour must be NotMocked when mockChannelsGet is false")
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
            val verifyPresenceLeave: Boolean,
            /**
             * If [GivenConfig.mockChannelsGet] is `false` then this must be `false`.
             */
            val verifyChannelUnsubscribeAndRelease: Boolean,
            val resultOfDisconnectCallOnObjectUnderTest: ThenTypes.ExpectedAsyncResult
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
                    if (verifyPresenceLeave) {
                        throw InvalidTestConfigurationException("verifyPresenceLeave must be false when mockChannelsGet is false")
                    }
                    if (verifyChannelOff) {
                        throw InvalidTestConfigurationException("verifyChannelOff must be false when mockChannelsGet is false")
                    }
                    if (verifyChannelUnsubscribeAndRelease) {
                        throw InvalidTestConfigurationException("verifyChannelUnsubscribeAndRelease must be false when mockChannelsGet is false")
                    }
                }
                if (givenConfig.channelStateChangeBehaviour !is GivenTypes.ChannelStateChangeBehaviour.EmitStateChange) {
                    if (verifyChannelStateChangeCurrent) {
                        throw InvalidTestConfigurationException("verifyChannelStateChangeCurrent must be false when channelStateChangeBehaviour is not EmitStateChange")
                    }
                }
            }
        }

        /**
         * Implements the following parameterised test case for [DefaultAbly.disconnect]:
         *
         * ```text
         * Given...
         *
         * ...that calling containsKey on the Channels instance returns ${givenConfig.channelsContainsKey}...
         *
         * if ${givenConfig.mockChannelsGet} {
         * ...and that calling `get` (the overload that does not accept a ChannelOptions object) on the Channels instance returns a channel in state ${givenConfig.initialChannelState}...
         * }
         *
         * when ${givenConfig.channelStateChangeBehaviour} is EmitStateChange {
         * ...which, when its `on` method is called, immediately calls the received listener with a channel state change whose `current` property is ${givenConfig.channelStateChangeBehaviour.current}...
         * }
         *
         * when ${givenConfig.presenceLeaveBehaviour} is Success {
         * ...[and] which, when told to leave presence, does so successfully...
         * }
         *
         * when ${givenConfig.presenceLeaveBehaviour} is Failure {
         * ...[and] which, when told to leave presence, fails to do so with error ${givenConfig.presenceLeaveBehaviour.errorInfo}...
         * }
         *
         * when ${givenConfig.presenceLeaveBehaviour} is DoesNotComplete {
         * ...[and] which, when told to leave presence, never finishes doing so...
         * }
         *
         * When...
         *
         * ...we call `disconnect` on the object under test,
         *
         * Then...
         * ...in the following order, precisely the following things happen...
         *
         * ...it calls `containsKey` on the Channels instance...
         *
         * if ${thenConfig.verifyChannelsGet} {
         * ...and calls `get` on the Channels instance...
         * }
         *
         * ...and fetches the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
         *
         * if ${thenConfig.verifyChannelOn} {
         * ...and calls `on` on the channel...
         * }
         *
         * if ${thenConfig.verifyChannelStateChangeCurrent} {
         * ...and checks the `current` property of the emitted channel state change...
         * }
         *
         *  if ${thenConfig.verifyChannelOff} {
         * ...and calls `off` on the channel...
         * }
         *
         * if ${thenConfig.verifyPresenceLeave} {
         * ...and tells the channel to leave presence...
         * }
         *
         * if ${thenConfig.verifyChannelUnsubscribeAndRelease} {
         * ...and calls `unsubscribe` on the channel and on its Presence instance...
         * ...and fetches the channel’s name and calls `release` on the Channels instance...
         * }
         *
         * when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.expectedResult} is Success {
         * ...and the call to `disconnect` (on the object under test) succeeds.
         * }
         *
         * when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
         * ...and the call to `disconnect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.errorInformation}.
         * }
         *
         * when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is DoesNotTerminate {
         * ...and the call to `disconnect` (on the object under test) does not complete within ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
         * }
         * ```
         */
        companion object {
            suspend fun test(givenConfig: GivenConfig, thenConfig: ThenConfig) {
                givenConfig.validate()
                thenConfig.validate(givenConfig)

                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
                val configuredChannel = testEnvironment.configuredChannels[0]

                // Given...

                // ...that calling containsKey on the Channels instance returns ${givenConfig.channelsContainsKey}...
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
                when (val givenChannelStateBehaviour = givenConfig.channelStateChangeBehaviour) {
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
                            configuredChannel.mockOnToEmitStateChange(current = givenChannelStateBehaviour.current)
                        configuredChannel.stubOff()
                    }
                }

                when (val givenPresenceLeaveBehaviour = givenConfig.presenceLeaveBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.presenceLeaveBehaviour} is Success {
                     * ...[and] which, when told to leave presence, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulPresenceLeave()
                    }
                    /* when ${givenConfig.presenceLeaveBehaviour} is Failure {
                     * ...[and] which, when told to leave presence, fails to do so with error ${givenConfig.presenceLeaveBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedPresenceLeave(givenPresenceLeaveBehaviour.errorInfo)
                    }
                    /* when ${givenConfig.presenceLeaveBehaviour} is DoesNotComplete {
                     * ...[and] which, when told to leave presence, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingPresenceLeave()
                    }
                }

                configuredChannel.stubUnsubscribe()
                configuredChannel.stubPresenceUnsubscribe()
                configuredChannel.mockName()
                testEnvironment.stubRelease(configuredChannel)

                // When...

                val result = executeForVerifying(thenConfig.resultOfDisconnectCallOnObjectUnderTest) {
                    // ...we call `disconnect` on the object under test,
                    testEnvironment.objectUnderTest.disconnect(
                        configuredChannel.trackableId,
                        PresenceData("")
                    )
                }

                // Then...
                // ...in the following order, precisely the following things happen...

                verifyOrder {
                    // ...it calls `containsKey` on the Channels instance...
                    testEnvironment.channelsMock.containsKey(configuredChannel.channelName)

                    if (thenConfig.verifyChannelsGet) {
                        /* if ${thenConfig.verifyChannelsGet} {
                         * ...and calls `get` on the Channels instance...
                         * }
                         */
                        testEnvironment.channelsMock.get(configuredChannel.channelName)
                    }

                    // ...and fetches the channel’s state ${thenConfig.numberOfChannelStateFetchesToVerify} times...
                    repeat(thenConfig.numberOfChannelStateFetchesToVerify) {
                        configuredChannel.channelMock.state
                    }

                    if (thenConfig.verifyChannelOn) {
                        /* if ${thenConfig.verifyChannelOn} {
                         * ...and calls `on` on the channel...
                         * }
                         */
                        configuredChannel.channelMock.on(any())
                    }

                    if (thenConfig.verifyChannelStateChangeCurrent) {
                        /* if ${thenConfig.verifyChannelStateChangeCurrent} {
                         * ...and checks the `current` property of the emitted channel state change...
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

                    if (thenConfig.verifyPresenceLeave) {
                        /* if ${thenConfig.verifyPresenceLeave} {
                         * ...and tells the channel to leave presence...
                         * }
                         */
                        configuredChannel.presenceMock.leave(any(), any())
                    }

                    if (thenConfig.verifyChannelUnsubscribeAndRelease) {
                        /* if ${thenConfig.verifyChannelUnsubscribeAndRelease} {
                         * ...and calls `unsubscribe` on the channel and on its Presence instance...
                         * ...and fetches the channel’s name and calls `release` on the Channels instance...
                         * }
                         */
                        configuredChannel.channelMock.unsubscribe()
                        configuredChannel.presenceMock.unsubscribe()
                        configuredChannel.channelMock.name
                        testEnvironment.channelsMock.release(configuredChannel.channelName)
                    }
                }

                /* when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.expectedResult} is Success {
                 * ...and the call to `disconnect` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
                 * ...and the call to `disconnect` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.errorInformation}.
                 * }
                 *
                 * when ${thenConfig.resultOfDisconnectCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `disconnect` (on the object under test) does not complete within ${thenConfig.resultOfDisconnectCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
                 * }
                 */
                thenConfig.resultOfDisconnectCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }

    /**
     * Provides test scenarios for [DefaultAbly.startConnection]. See the [Companion.test] method.
     */
    class StartConnection {
        /**
         * This class provides properties for configuring the "Given..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class GivenConfig(
            val initialConnectionState: ConnectionState,
            val connectionReasonBehaviour: GivenTypes.ConnectionReasonMockBehaviour,
            val connectBehaviour: GivenTypes.ConnectionStateChangeBehaviour
        )

        /**
         * This class provides properties for configuring the "Then..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class ThenConfig(
            val numberOfConnectionStateFetchesToVerify: Int,
            val verifyConnectionReasonFetch: Boolean,
            val verifyConnectionOn: Boolean,
            val verifyConnect: Boolean,
            val verifyConnectionOff: Boolean,
            val resultOfStartConnectionCallOnObjectUnderTest: ThenTypes.ExpectedResult
        )

        companion object {
            /**
             * Implements the following parameterised test case for [DefaultAbly.startConnection]:
             *
             * ```text
             * Given...
             *
             * ...that the connection’s `state` property returns ${givenConfig.initialConnectionState}...
             *
             * when ${givenConfig.connectionReasonBehaviour} is Mocked {
             * ...and that the connection’s `reason` property returns ${givenConfig.connectionReasonBehaviour}...
             * }
             *
             * when ${givenConfig.connectBehaviour} is EmitStateChange {
             * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous`, `current`, `retryIn` and `reason` are those of ${givenConfig.connectBehaviour}...
             * }
             *
             * When...
             *
             * ...the `startConnection` method is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state ${thenConfig.numberOfConnectionStateFetchesToVerify} times...
             *
             * if ${thenConfig.verifyConnectionReasonFetch} {
             * ...and fetches the connection’s `reason`...
             * }
             *
             * if ${thenConfig.verifyConnectionOn} {
             * ...and adds a listener to the connection using `on`...
             * }
             *
             * if ${thenConfig.verifyConnect} {
             * ...and tells the Realtime instance to connect...
             * }
             *
             * if ${thenConfig.verifyConnectionOff} {
             * ...and removes a listener from the connection using `off`...
             * }
             *
             * when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.expectedResult} is Success {
             * ...and the call to `startConnection` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
             * ...and the call to `startConnection` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.errorInformation}.
             * }
             *
             * when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is DoesNotTerminate {
             * ...and the call to `startConnection` (on the object under test) does not complete within ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
             * }
             * ```
             */
            suspend fun test(givenConfig: GivenConfig, thenConfig: ThenConfig) {
                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 0)

                // Given...

                // ...that the connection’s `state` property returns ${givenConfig.initialConnectionState}...
                testEnvironment.mockConnectionState(givenConfig.initialConnectionState)

                when (val givenConnectionReasonBehaviour = givenConfig.connectionReasonBehaviour) {
                    is GivenTypes.ConnectionReasonMockBehaviour.NotMocked -> {}
                    is GivenTypes.ConnectionReasonMockBehaviour.Mocked -> {
                        /* when ${givenConfig.connectionReasonBehaviour} is Mocked {
                         * ...and that the connection’s `reason` property returns ${givenConfig.connectionReasonBehaviour}...
                         * }
                         */
                        testEnvironment.mockConnectionReason(givenConnectionReasonBehaviour.reason)
                    }
                }

                when (val givenConnectBehaviour = givenConfig.connectBehaviour) {
                    is GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour -> {
                        testEnvironment.stubConnectionOn()
                        testEnvironment.stubConnect()
                    }
                    is GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange -> {
                        /* when ${givenConfig.connectBehaviour} is EmitStateChange {
                         * ...and that when the Realtime instance’s `connect` method is called, its connection’s `on` method immediately emits a connection state change whose `previous`, `current`, `retryIn` and `reason` are those of ${givenConfig.connectBehaviour}...
                         * }
                         */
                        testEnvironment.mockConnectToEmitStateChange(
                            previous = givenConnectBehaviour.previous,
                            current = givenConnectBehaviour.current,
                            retryIn = givenConnectBehaviour.retryIn,
                            reason = givenConnectBehaviour.reason
                        )
                    }
                }

                testEnvironment.stubConnectionOff()

                // When...

                // ...the `startConnection` method is called on the object under test...
                val result = testEnvironment.objectUnderTest.startConnection()

                // Then...
                // ...in the following order, precisely the following things happen...

                verifyOrder {
                    // ...it fetches the connection’s state ${thenConfig.numberOfConnectionStateFetchesToVerify} times...
                    repeat(thenConfig.numberOfConnectionStateFetchesToVerify) {
                        testEnvironment.connectionMock.state
                    }

                    if (thenConfig.verifyConnectionReasonFetch) {
                        /* if ${thenConfig.verifyConnectionReasonFetch} {
                         * ...and fetches the connection’s `reason`...
                         * }
                         */
                        testEnvironment.connectionMock.reason
                    }

                    if (thenConfig.verifyConnectionOn) {
                        /* if ${thenConfig.verifyConnectionOn} {
                         * ...and adds a listener to the connection using `on`...
                         * }
                         */
                        testEnvironment.connectionMock.on(any())
                    }

                    if (thenConfig.verifyConnect) {
                        /* if ${thenConfig.verifyConnect} {
                         * ...and tells the Realtime instance to connect...
                         * }
                         */
                        testEnvironment.realtimeMock.connect()
                    }

                    if (thenConfig.verifyConnectionOff) {
                        /* if ${thenConfig.verifyConnectionOff} {
                         * ...and removes a listener from the connection using `off`...
                         * }
                         */
                        testEnvironment.connectionMock.off(any())
                    }
                }

                /* when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.expectedResult} is Success {
                 * ...and the call to `startConnection` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
                 * ...and the call to `startConnection` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.errorInformation}.
                 * }
                 *
                 * when ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `startConnection` (on the object under test) does not complete within ${thenConfig.resultOfStartConnectionCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
                 * }
                 */
                thenConfig.resultOfStartConnectionCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }

    /**
     * Provides test scenarios for [DefaultAbly.stopConnection]. See the [Companion.test] method.
     */
    class StopConnection {
        /**
         * This class provides properties for configuring the "Given..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class GivenConfig(
            val initialConnectionState: ConnectionState,
            val closeBehaviour: GivenTypes.ConnectionStateChangeBehaviour
        )

        /**
         * This class provides properties for configuring the "Then..." part of the parameterised test case described by [Companion.test]. See that method’s documentation for information about the effect of this class’s properties.
         */
        class ThenConfig(
            val numberOfConnectionStateFetchesToVerify: Int,
            val verifyConnectionOn: Boolean,
            val verifyClose: Boolean,
            val verifyConnectionOff: Boolean,
            val resultOfStopConnectionCallOnObjectUnderTest: ThenTypes.ExpectedAsyncResult
        ) {
            /**
             * Checks that this object represents a valid test configuration.
             *
             * @throws InvalidTestConfigurationException If this object does not represent a valid test configuration.
             */
            fun validate() {
                if (resultOfStopConnectionCallOnObjectUnderTest is ThenTypes.ExpectedAsyncResult.Terminates && resultOfStopConnectionCallOnObjectUnderTest.expectedResult is ThenTypes.ExpectedResult.FailureWithConnectionException) {
                    throw InvalidTestConfigurationException("resultOfStopConnectionCallOnObjectUnderTest.expectedResult must not be FailureWithConnectionException")
                }
            }
        }

        companion object {
            /**
             * Implements the following parameterised test case for [DefaultAbly.stopConnection]:
             *
             * ```text
             * Given...
             *
             * ...that the connection’s `state` property returns ${givenConfig.initialConnectionState}...
             *
             * when ${givenConfig.closeBehaviour} is EmitStateChange {
             * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous`, `current`, `retryIn` and `reason` are those of ${givenConfig.closeBehaviour}...
             * }
             *
             * When...
             *
             * ...`stopConnection` is called on the object under test...
             *
             * Then...
             * ...in the following order, precisely the following things happen...
             *
             * ...it fetches the connection’s state ${thenConfig.numberOfConnectionStateFetchesToVerify} times...
             *
             * if ${thenConfig.verifyConnectionOn} {
             * ...and adds a listener to the connection using `on`...
             * }
             *
             * if ${thenConfig.verifyClose} {
             * ...and tells the Realtime instance to close...
             * }
             *
             * if ${thenConfig.verifyConnectionOff} {
             * ...and removes a listener from the connection using `off`...
             * }
             *
             * when ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest.result} is Success {
             * ...and the call to `stopConnection` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} is DoesNotTerminate {
             * ...and the call to `stopConnection` (on the object under test) does not complete within ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} milliseconds.
             * }
             * ```
             */
            suspend fun test(
                givenConfig: GivenConfig,
                thenConfig: ThenConfig
            ) {
                thenConfig.validate()

                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 0)

                // Given...

                // ...that the connection’s `state` property returns ${givenConfig.initialConnectionState}...
                testEnvironment.mockConnectionState(givenConfig.initialConnectionState)

                when (val givenCloseBehaviour = givenConfig.closeBehaviour) {
                    is GivenTypes.ConnectionStateChangeBehaviour.NoBehaviour -> {
                        testEnvironment.stubConnectionOn()
                        testEnvironment.stubClose()
                    }
                    is GivenTypes.ConnectionStateChangeBehaviour.EmitStateChange -> {
                        /* when ${givenConfig.closeBehaviour} is EmitStateChange {
                         * ...and that when the Realtime instance’s `close` method is called, its connection’s `on` method immediately emits a connection state change whose `previous`, `current`, `retryIn` and `reason` are those of ${givenConfig.closeBehaviour}...
                         * }
                         */
                        testEnvironment.mockCloseToEmitStateChange(
                            previous = givenCloseBehaviour.previous,
                            current = givenCloseBehaviour.current,
                            retryIn = givenCloseBehaviour.retryIn,
                            reason = givenCloseBehaviour.reason
                        )
                    }
                }

                testEnvironment.stubConnectionOff()

                // When...

                val result = executeForVerifying(thenConfig.resultOfStopConnectionCallOnObjectUnderTest) {
                    // ...`stopConnection` is called on the object under test...
                    testEnvironment.objectUnderTest.stopConnection()
                }

                // Then...
                // ...in the following order, precisely the following things happen...

                verifyOrder {
                    // ...it fetches the connection’s state ${thenConfig.numberOfConnectionStateFetchesToVerify} times...
                    repeat(thenConfig.numberOfConnectionStateFetchesToVerify) {
                        testEnvironment.connectionMock.state
                    }

                    if (thenConfig.verifyConnectionOn) {
                        /* if ${thenConfig.verifyConnectionOn} {
                         * ...and adds a listener to the connection using `on`...
                         * }
                         */
                        testEnvironment.connectionMock.on(any())
                    }

                    if (thenConfig.verifyClose) {
                        /* if ${thenConfig.verifyClose} {
                         * ...and tells the Realtime instance to close...
                         * }
                         */
                        testEnvironment.realtimeMock.close()
                    }

                    if (thenConfig.verifyConnectionOff) {
                        /* if ${thenConfig.verifyConnectionOff} {
                         * ...and removes a listener from the connection using `off`...
                         * }
                         */
                        testEnvironment.connectionMock.off(any())
                    }
                }

                /* when ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest.result} is Success {
                 * ...and the call to `stopConnection` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `stopConnection` (on the object under test) does not complete within ${thenConfig.resultOfStopConnectionCallOnObjectUnderTest} milliseconds.
                 * }
                 */
                thenConfig.resultOfStopConnectionCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }

    /**
     * Provides test scenarios for [DefaultAbly.sendRawLocation]. See the [Companion.test] method.
     */
    class SendRawLocation {
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
            val publishBehaviour: GivenTypes.CompletionListenerMockBehaviour
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
                    if (publishBehaviour !is GivenTypes.CompletionListenerMockBehaviour.NotMocked) {
                        throw InvalidTestConfigurationException("publishBehaviour must be NotMocked when mockChannelsGet is false")
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
            val verifyPublish: Boolean,
            val resultOfSendRawLocationCallOnObjectUnderTest: ThenTypes.ExpectedAsyncResult,
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
                        throw InvalidTestConfigurationException("verifyChannelOff must be false when mockChannelsGet is false")
                    }
                    if (verifyPublish) {
                        throw InvalidTestConfigurationException("verifyPublish must be false when mockChannelsGet is false")
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
             * Implements the following parameterised test case for [DefaultAbly.sendRawLocation]:
             *
             * ```text
             * Given...
             *
             * ...that the Channels instance’s `containsKey` method returns ${givenConfig.channelsContainsKey}...
             *
             * if ${givenConfig.mockChannelsGet} {
             * ...and that the Channels instance’s `get` method (the overload that does not accept a ChannelOptions object) returns a channel in the ${givenConfig.initialChannelState} state...
             * }
             *
             * when ${givenConfig.channelStateChangeBehaviour} is EmitStateChange {
             * ...which, when its `on` method is called, immediately calls the received listener with a channel state change whose `current` property is ${givenConfig.channelStateChangeBehaviour.current}...
             * }
             *
             * when ${givenConfig.publishBehaviour} is Success {
             * ...[and] which, when told to publish a message, does so successfully...
             * }
             *
             * when ${givenConfig.publishBehaviour} is Failure {
             * ...[and] which, when told to publish a message, fails to do so with error ${givenConfig.publishBehaviour.errorInfo}...
             * }
             *
             * when ${givenConfig.publishBehaviour} is DoesNotComplete {
             * ...[and] which, when told to publish a message, never finishes doing so...
             * }
             *
             * When...
             *
             * ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
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
             * if ${thenConfig.verifyChannelStateChangeCurrent} {
             * ...and checks the `current` property of the emitted channel state change...
             * }
             *
             * if ${thenConfig.verifyChannelOff} {
             * ...and calls `off` on the channel...
             * }
             *
             * if ${thenConfig.verifyPublish} {
             * ...and tells the channel to publish a message whose `name` property is "raw"...
             * }
             *
             * when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.expectedResult} is Success {
             * ...and the call to `sendRawLocation` (on the object under test) succeeds.
             * }
             *
             * when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
             * ...and the call to `sendRawLocation` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.errorInformation}.
             * }
             *
             * when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is DoesNotTerminate {
             * ...and the call to `sendRawLocation` (on the object under test) does not complete within ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
             * }
             * ```
             */
            suspend fun test(
                givenConfig: GivenConfig,
                thenConfig: ThenConfig
            ) {
                givenConfig.validate()
                thenConfig.validate(givenConfig)

                val testEnvironment = DefaultAblyTestEnvironment.create(numberOfTrackables = 1)
                val configuredChannel = testEnvironment.configuredChannels[0]

                // Given...

                // ...that the Channels instance’s `containsKey` method returns ${givenConfig.channelsContainsKey}...
                testEnvironment.mockChannelsContainsKey(
                    configuredChannel.channelName,
                    givenConfig.channelsContainsKey
                )

                if (givenConfig.mockChannelsGet) {
                    /* if ${givenConfig.mockChannelsGet} {
                     * ...and that the Channels instance’s `get` method (the overload that does not accept a ChannelOptions object) returns a channel in the ${givenConfig.initialChannelState} state...
                     * }
                     */
                    testEnvironment.mockChannelsGet(DefaultAblyTestEnvironment.ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS)
                    configuredChannel.mockState(givenConfig.initialChannelState!!)
                }

                val channelStateChangeMock: AblySdkChannelStateListener.ChannelStateChange?
                when (val givenChannelStateBehaviour = givenConfig.channelStateChangeBehaviour) {
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
                            configuredChannel.mockOnToEmitStateChange(current = givenChannelStateBehaviour.current)
                        configuredChannel.stubOff()
                    }
                }

                when (val givenPublishBehaviour = givenConfig.publishBehaviour) {
                    is GivenTypes.CompletionListenerMockBehaviour.NotMocked -> {}
                    /* when ${givenConfig.publishBehaviour} is Success {
                     * ...[and] which, when told to publish a message, does so successfully...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Success -> {
                        configuredChannel.mockSuccessfulPublish()
                    }
                    /* when ${givenConfig.publishBehaviour} is Failure {
                     * ...[and] which, when told to publish a message, fails to do so with error ${givenConfig.publishBehaviour.errorInfo}...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.Failure -> {
                        configuredChannel.mockFailedPublish(givenPublishBehaviour.errorInfo)
                    }
                    /* when ${givenConfig.publishBehaviour} is DoesNotComplete {
                     * ...[and] which, when told to publish a message, never finishes doing so...
                     * }
                     */
                    is GivenTypes.CompletionListenerMockBehaviour.DoesNotComplete -> {
                        configuredChannel.mockNonCompletingPublish()
                    }
                }

                // When...

                val result = executeForVerifying(thenConfig.resultOfSendRawLocationCallOnObjectUnderTest) {
                    suspendCancellableCoroutine<Result<Unit>> { continuation ->
                        // ...we call `sendRawLocation` on the object under test (with an arbitrarily-chosen LocationUpdate argument),
                        testEnvironment.objectUnderTest.sendRawLocation(
                            configuredChannel.trackableId,
                            LocationUpdate(
                                Location(0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0),
                                listOf()
                            )
                        ) { result ->
                            continuation.resume(result)
                        }
                    }
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
                         * }
                         */
                        configuredChannel.channelMock.on(any())
                    }

                    if (thenConfig.verifyChannelStateChangeCurrent) {
                        /* if ${thenConfig.verifyChannelStateChangeCurrent} {
                         * ...and checks the `current` property of the emitted channel state change...
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

                    if (thenConfig.verifyPublish) {
                        /* if ${thenConfig.verifyPublish} {
                         * ...and tells the channel to publish a message whose `name` property is "raw"...
                         * }
                         */
                        configuredChannel.channelMock.publish(
                            message = match { it.name == "raw" },
                            any()
                        )
                    }
                }

                /* when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.expectedResult} is Success {
                 * ...and the call to `sendRawLocation` (on the object under test) succeeds.
                 * }
                 *
                 * when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is Terminates and ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.expectedResult} is FailureWithConnectionException {
                 * ...and the call to `sendRawLocation` (on the object under test) fails with a ConnectionException whose errorInformation is equal to ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.errorInformation}.
                 * }
                 *
                 * when ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest} is DoesNotTerminate {
                 * ...and the call to `sendRawLocation` (on the object under test) does not complete within ${thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.timeoutInMilliseconds} milliseconds.
                 * }
                 */
                thenConfig.resultOfSendRawLocationCallOnObjectUnderTest.verify(result)

                confirmVerified(*testEnvironment.allMocks)
            }
        }
    }
}
