package com.ably.tracking.common.helper

import com.ably.tracking.common.AblySdkChannelStateListener
import com.ably.tracking.common.AblySdkRealtime
import com.ably.tracking.common.AblySdkFactory
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.ChannelOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.excludeRecords
import io.mockk.confirmVerified
import io.mockk.verifySequence
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import io.ably.lib.types.ErrorInfo

/**
 * Provides an environment for creating and testing an instance of [DefaultAbly].
 *
 * It creates mock objects to represent the components of the Ably client library, and uses these to configure and create an instance of [DefaultAbly].
 * It exposes this instance as the [objectUnderTest] property, and also exposes the configured mocks so that each test case can perform verifications on them.
 */
class DefaultAblyTestEnvironment private constructor(
    /**
     * The [DefaultAbly] object created by [create].
     * It has been configured with an [AblySdkFactory] mock that supplies [realtimeMock].
     */
    val objectUnderTest: DefaultAbly<AblySdkChannelStateListener>,
    /**
     * The [AblySdkRealtime] mock created by [create].
     * Its [AblySdkRealtime.connection] property returns [connectionMock], and its [AblySdkRealtime.channels] property returns [channelsMock]. Calls to these property getters are ignored by MockK’s [confirmVerified] method (by use of [excludeRecords]).
     */
    val realtimeMock: AblySdkRealtime<AblySdkChannelStateListener>,
    /**
     * The [AblySdkRealtime.Connection] mock which [create] has created and configured [realtimeMock]’s [AblySdkRealtime.connection] property to return.
     */
    val connectionMock: AblySdkRealtime.Connection,
    /**
     * The [AblySdkRealtime.Channels] mock which [create] has created and configured [realtimeMock]’s [AblySdkRealtime.channels] property to return.
     * See the documentation for that method for details of how this mock is configured.
     */
    val channelsMock: AblySdkRealtime.Channels<AblySdkChannelStateListener>,
    /**
     * The set of [AblySdkRealtime.Channel] mocks that [create] has configured [channelsMock] to supply.
     * See the documentation for that method for details of how these mocks are configured.
     */
    val configuredChannels: List<ConfiguredChannel>
) {
    /**
     * The details of a an [AblySdkRealtime.Channel] mock that [create] has configured [channelsMock] to supply.
     */
    class ConfiguredChannel(
        /**
         * The ID of the trackable to which this mocked channel corresponds. This is provided as a convenience for test cases.
         */
        val trackableId: String,
        /**
         * The name of the channel to which [channelMock] corresponds.
         */
        val channelName: String,
        /**
         * The [AblySdkRealtime.Channel] mock created by [create].
         * See the documentation for that method for details of how this mock is configured.
         * Its [AblySdkRealtime.Channel.presence] property returns [presenceMock]. Calls to this property getter are ignored by MockK’s [confirmVerified] method (by use of [excludeRecords]).
         */
        val channelMock: AblySdkRealtime.Channel<AblySdkChannelStateListener>,
        /**
         * The [AblySdkRealtime.Presence] mock which [create] has created and configured [channelMock]’s [AblySdkRealtime.Channel.presence] property to return.
         */
        val presenceMock: AblySdkRealtime.Presence
    ) {
        /**
         * Contains the [AblySdkChannelStateListener.ChannelStateChange] MockK mocks created by [mockOnToEmitStateChange].
         */
        private var stateChangeMocks =
            mutableListOf<AblySdkChannelStateListener.ChannelStateChange>()

        /**
         * A list of all of the MockK mock objects that this object has created.
         */
        val allMocks: List<Any>
            get() {
                return listOf(channelMock, presenceMock) + stateChangeMocks
            }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.name] property to return [channelName].
         */
        fun mockName() {
            every { channelMock.name } returns channelName
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.state] property to return [state].
         *
         * @param state The state that [channelMock]’s [AblySdkRealtime.Channel.state] property should return.
         */
        fun mockState(state: ChannelState) {
            every { channelMock.state } returns state
        }

        /**
         * Stubs [channelMock]’s [AblySdkRealtime.Channel.unsubscribe] method.
         */
        fun stubUnsubscribe() {
            every { channelMock.unsubscribe() } returns Unit
        }

        /**
         * Stubs [presenceMock]’s [AblySdkRealtime.Presence.unsubscribe] method.
         */
        fun stubPresenceUnsubscribe() {
            every { presenceMock.unsubscribe() } returns Unit
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.enter] method to immediately pass its received completion listener to [handler].
         *
         * @param handler The function that should receive the completion listener passed to [presenceMock]’s [AblySdkRealtime.Presence.enter] method.
         */
        private fun mockPresenceEnterResult(handler: (CompletionListener) -> Unit) {
            val completionListenerSlot = slot<CompletionListener>()
            every {
                presenceMock.enter(
                    any(),
                    capture(completionListenerSlot)
                )
            } answers { handler(completionListenerSlot.captured) }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.enter] method to immediately call its received completion listener’s [CompletionListener.onSuccess] method.
         */
        fun mockSuccessfulPresenceEnter() {
            mockPresenceEnterResult { it.onSuccess() }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.enter] method to immediately call its received completion listener’s [CompletionListener.onError] method.
         *
         * @param errorInfo The error that should be passed to the completion listener’s [CompletionListener.onError] method.
         */
        fun mockFailedPresenceEnter(errorInfo: ErrorInfo) {
            mockPresenceEnterResult { it.onError(errorInfo) }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.enter] method to never call any methods on its received completion listener.
         */
        fun mockNonCompletingPresenceEnter() {
            mockPresenceEnterResult { }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.update] method to immediately pass its received completion listener to [handler].
         *
         * @param handler The function that should receive the completion listener passed to [presenceMock]’s [AblySdkRealtime.Presence.update] method.
         */
        private fun mockPresenceUpdateResult(handler: (CompletionListener) -> Unit) {
            val completionListenerSlot = slot<CompletionListener>()
            every {
                presenceMock.update(
                    any(),
                    capture(completionListenerSlot)
                )
            } answers { handler(completionListenerSlot.captured) }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.update] method to immediately call its received completion listener’s [CompletionListener.onSuccess] method.
         */
        fun mockSuccessfulPresenceUpdate() {
            mockPresenceUpdateResult { it.onSuccess() }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.update] method to immediately call its received completion listener’s [CompletionListener.onError] method.
         *
         * @param errorInfo The error that should be passed to the completion listener’s [CompletionListener.onError] method.
         */
        fun mockFailedPresenceUpdate(errorInfo: ErrorInfo) {
            mockPresenceUpdateResult { it.onError(errorInfo) }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.update] method to never call any methods on its received completion listener.
         */
        fun mockNonCompletingPresenceUpdate() {
            mockPresenceUpdateResult { }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.leave] method to immediately pass its received completion listener to [handler].
         *
         * @param handler The function that should receive the completion listener passed to [presenceMock]’s [AblySdkRealtime.Presence.leave] method.
         */
        private fun mockPresenceLeaveResult(handler: (CompletionListener) -> Unit) {
            val completionListenerSlot = slot<CompletionListener>()
            every {
                presenceMock.leave(
                    any(),
                    capture(completionListenerSlot)
                )
            } answers {
                handler(completionListenerSlot.captured)
            }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.leave] method to immediately call its received completion listener’s [CompletionListener.onSuccess] method.
         */
        fun mockSuccessfulPresenceLeave() {
            mockPresenceLeaveResult { it.onSuccess() }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.leave] method to immediately call its received completion listener’s [CompletionListener.onError] method.
         *
         * @param errorInfo The error that should be passed to the completion listener’s [CompletionListener.onError] method.
         */
        fun mockFailedPresenceLeave(errorInfo: ErrorInfo) {
            mockPresenceLeaveResult { it.onError(errorInfo) }
        }

        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.leave] method to never call any methods on its received completion listener.
         */
        fun mockNonCompletingPresenceLeave() {
            mockPresenceLeaveResult { }
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.attach] method to immediately pass its received completion listener to [handler].
         *
         * @param handler The function that should receive the completion listener passed to [channelMock]’s [AblySdkRealtime.Channel.attach] method.
         */
        private fun mockAttachResult(handler: (CompletionListener) -> Unit) {
            val completionListenerSlot = slot<CompletionListener>()
            every { channelMock.attach(capture(completionListenerSlot)) } answers {
                handler(
                    completionListenerSlot.captured
                )
            }
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.attach] method to immediately call its received completion listener’s [CompletionListener.onSuccess] method.
         */
        fun mockSuccessfulAttach() {
            mockAttachResult { it.onSuccess() }
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.attach] method to immediately call its received completion listener’s [CompletionListener.onError] method.
         *
         * @param errorInfo The error that should be passed to the completion listener’s [CompletionListener.onError] method.
         */
        fun mockFailedAttach(errorInfo: ErrorInfo) {
            mockAttachResult { it.onError(errorInfo) }
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.attach] method to never call any methods on its received completion listener.
         */
        fun mockNonCompletingAttach() {
            mockAttachResult { }
        }

        /**
         * Stubs [channelMock]’s [AblySdkRealtime.Channel.on] method.
         */
        fun stubOn() {
            every { channelMock.on(any()) } returns Unit
        }

        /**
         * Stubs [channelMock]’s [AblySdkRealtime.Channel.off] method (the overload which accepts an [AblySdkChannelStateListener] object).
         */
        fun stubOff() {
            every { channelMock.off(any()) } returns Unit
        }

        /**
         * Mocks [channelMock]’s [AblySdkRealtime.Channel.on] method to capture the received [AblySdkChannelStateListener], and to then immediately call this listener with a [AblySdkChannelStateListener.ChannelStateChange] MockK mock constructed from the [current] argument.
         *
         * @param current The value that the state change’s [ConnectionStateListener.ConnectionStateChange.current] property will return.
         * @return The [AblySdkChannelStateListener.ChannelStateChange] MockK mock that will be passed to the listener.
         */
        fun mockOnToEmitStateChange(current: ChannelState): AblySdkChannelStateListener.ChannelStateChange {
            val listenerSlot = slot<AblySdkChannelStateListener>()

            val stateChangeMock = mockk<AblySdkChannelStateListener.ChannelStateChange>()
            stateChangeMocks.add(stateChangeMock)
            every { stateChangeMock.current } returns current

            every { channelMock.on(capture(listenerSlot)) } answers {
                listenerSlot.captured.onChannelStateChanged(stateChangeMock)
            }

            return stateChangeMock
        }
    }

    /**
     * Describes an overload of [AblySdkRealtime.Channels.get].
     */
    enum class ChannelsGetOverload {
        /**
         * The overload of [AblySdkRealtime.Channels.get] which does not accept a [ChannelOptions] object.
         */
        WITHOUT_CHANNEL_OPTIONS,

        /**
         * The overload of [AblySdkRealtime.Channels.get] which accepts a [ChannelOptions] object.
         */
        WITH_CHANNEL_OPTIONS,
    }

    /**
     * Mocks [channelsMock]’s [AblySdkRealtime.Channels.get] method to return [configuredChannel]’s [ConfiguredChannel.channelMock].
     *
     * @param configuredChannel The object whose [ConfiguredChannel.channelMock] should be used.
     * @param overload Specifies which overload of `get` to mock.
     */
    private fun mockChannelsGet(
        configuredChannel: ConfiguredChannel,
        overload: ChannelsGetOverload
    ) {
        when (overload) {
            ChannelsGetOverload.WITHOUT_CHANNEL_OPTIONS -> {
                every {
                    channelsMock.get(configuredChannel.channelName)
                } returns configuredChannel.channelMock
            }
            ChannelsGetOverload.WITH_CHANNEL_OPTIONS -> {
                every {
                    channelsMock.get(configuredChannel.channelName, any())
                } returns configuredChannel.channelMock
            }
        }
    }

    /**
     * For each `configuredChannel` in [configuredChannels], mocks [channelsMock]’s [AblySdkRealtime.Channels.get] method to return `configuredChannel`’s [ConfiguredChannel.channelMock].
     *
     * @param overload Specifies which overload of `get` to mock.
     */
    fun mockChannelsGet(overload: ChannelsGetOverload) {
        for (configuredChannel in configuredChannels) {
            mockChannelsGet(configuredChannel, overload)
        }
    }

    /**
     * Mocks [channelsMock]’s [AblySdkRealtime.Channels.containsKey] method to return [result] for key [key].
     *
     * @param result The result that [channelsMock]’s [AblySdkRealtime.Channels.containsKey] method should return for key [key].
     * @param key The key for which [result] should be returned.
     */
    fun mockChannelsContainsKey(key: Any, result: Boolean) {
        every { channelsMock.containsKey(key) } returns result
    }

    /**
     * An array of all of the MockK mock objects that this test environment has created.
     *
     * By passing this array to [confirmVerified], you can confirm that the mock verifications that your test makes using MockK’s `verify*` methods contain a comprehensive list of all the methods called on the mocks created by this test environment. This is a more comprehensive approach than using, for example, [verifySequence], which only verifies the mock objects which are mentioned inside its `verifyBlock`.
     */
    val allMocks: Array<Any>
        get() {
            val list = listOf(
                realtimeMock,
                connectionMock,
                channelsMock
            ) + configuredChannels.flatMap { it.allMocks }

            return list.toTypedArray()
        }

    /**
     * Mocks [channelsMock]’s [AblySdkRealtime.Channels.entrySet] method to return the list of channel mocks currently contained in [configuredChannels].
     */
    fun mockChannelsEntrySet() {
        val entrySet = configuredChannels.map { configuredChannel ->
            object : Map.Entry<String, AblySdkRealtime.Channel<AblySdkChannelStateListener>> {
                override val key = configuredChannel.channelName
                override val value = configuredChannel.channelMock
            }
        }
        every { channelsMock.entrySet() } returns entrySet
    }

    /**
     * Stubs [channelMock]’s [AblySdkRealtime.Channels.release] method for the channel named by [configuredChannel]’s [ConfiguredChannel.channelName] property.
     *
     * @param configuredChannel The object whose [ConfiguredChannel.channelName] property should be used.
     */
    fun stubRelease(configuredChannel: ConfiguredChannel) {
        every { channelsMock.release(configuredChannel.channelName) } returns Unit
    }

    /**
     * Mocks [connectionMock]’s [AblySdkRealtime.Connection.state] property to return [state].
     *
     * @param state The connection state to return.
     */
    fun mockConnectionState(state: ConnectionState) {
        every { connectionMock.state } returns state
    }

    /**
     * Mocks [connectionMock]’s [AblySdkRealtime.Connection.on] method to capture the received [ConnectionStateListener], and mocks [realtimeMock]’s [AblySdkRealtime.close] method to immediately call this listener with a [ConnectionStateListener.ConnectionStateChange] object constructed from the [previous], [current], [retryIn] and [reason] arguments.
     *
     * @param previous The value to be used as the `previous` parameter of [ConnectionStateListener.ConnectionStateChange]’s constructor.
     * @param current The value to be used as the `current` parameter of [ConnectionStateListener.ConnectionStateChange]’s constructor.
     * @param retryIn The value to be used as the `retryIn` parameter of [ConnectionStateListener.ConnectionStateChange]’s constructor.
     * @param reason The value to be used as the `reason` parameter of [ConnectionStateListener.ConnectionStateChange]’s constructor.
     */
    fun mockCloseToEmitStateChange(
        previous: ConnectionState,
        current: ConnectionState,
        retryIn: Long,
        reason: ErrorInfo?
    ) {
        val connectionStateListenerSlot = slot<ConnectionStateListener>()
        every { connectionMock.on(capture(connectionStateListenerSlot)) } returns Unit

        every { realtimeMock.close() } answers {
            val connectionStateChange = ConnectionStateListener.ConnectionStateChange(
                previous, current, retryIn, reason
            )
            connectionStateListenerSlot.captured.onConnectionStateChanged(connectionStateChange)
        }
    }

    /**
     * Stubs [connectionMock]’s [AblySdkRealtime.Connection.off] method.
     */
    fun stubConnectionOff() {
        every { connectionMock.off(any()) } returns Unit
    }

    companion object {
        /**
         * Creates an instance of [DefaultAblyTestEnvironment], with some basic initial configuration applied to its mocks.
         *
         * Specifically:
         *
         * 1. It creates [numberOfTrackables] channel mocks, and configures each of their [AblySdkRealtime.Channel.state] properties to return [ChannelState.initialized].
         *    It exposes these channel mocks via the [configuredChannels] property, each with a different [ConfiguredChannel.name] value, each of which have a `"tracking:"` prefix.
         * 1. It creates a [AblySdkRealtime.Channels] mock.
         * 1. It creates a [DefaultAbly] object using the mocks described above and in the documentation for the properties of [DefaultAblyTestEnvironment].
         *
         *    It provides arbitrary values for required parameters that we here consider to be unimportant – namely the `connectionConfiguration` and `logHandler` parameters of [DefaultAbly]’s constructor.
         *
         * @param numberOfTrackables The number of channel mocks to create.
         *
         * @return A configured [DefaultAblyTestEnvironment] object.
         */
        fun create(numberOfTrackables: Int): DefaultAblyTestEnvironment {
            val configuredChannels = (numberOfTrackables downTo 1).map { i ->
                val trackableId = "someTrackable-$i"
                val channelName = "tracking:$trackableId"

                val presenceMock = mockk<AblySdkRealtime.Presence>()

                val channelMock = mockk<AblySdkRealtime.Channel<AblySdkChannelStateListener>>()
                every { channelMock.state } returns ChannelState.initialized
                every { channelMock.presence } returns presenceMock
                excludeRecords { channelMock.presence }

                ConfiguredChannel(trackableId, channelName, channelMock, presenceMock)
            }

            val channelsMock = mockk<AblySdkRealtime.Channels<AblySdkChannelStateListener>>()
            val connectionMock = mockk<AblySdkRealtime.Connection>()

            val realtimeMock = mockk<AblySdkRealtime<AblySdkChannelStateListener>>()
            every { realtimeMock.channels } returns channelsMock
            excludeRecords { realtimeMock.channels }
            every { realtimeMock.connection } returns connectionMock
            excludeRecords { realtimeMock.connection }

            val factory = mockk<AblySdkFactory<AblySdkChannelStateListener>>()
            every { factory.createRealtime(any()) } returns realtimeMock

            // We provide an implementation of wrapChannelStateListener whose return value just calls the underlying listener.
            every { factory.wrapChannelStateListener(any()) } answers {
                val underlyingListener =
                    arg(0) as AblySdkFactory.UnderlyingChannelStateListener<AblySdkChannelStateListener>
                val wrapper = mockk<AblySdkChannelStateListener>()

                every { wrapper.onChannelStateChanged(any()) } answers {
                    val stateChange = arg(0) as AblySdkChannelStateListener.ChannelStateChange
                    underlyingListener.onChannelStateChanged(wrapper, stateChange)
                }

                wrapper
            }

            val connectionConfiguration =
                ConnectionConfiguration(Authentication.basic("", "")) // arbitrarily chosen
            val objectUnderTest =
                DefaultAbly(factory, connectionConfiguration, null /* arbitrarily chosen */)

            return DefaultAblyTestEnvironment(
                objectUnderTest,
                realtimeMock,
                connectionMock,
                channelsMock,
                configuredChannels
            )
        }
    }
}
