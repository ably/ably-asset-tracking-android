package com.ably.tracking.common.helper

import com.ably.tracking.common.AblySdkRealtime
import com.ably.tracking.common.AblySdkRealtimeFactory
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

/**
 * Provides an environment for creating and testing an instance of [DefaultAbly].
 *
 * It creates mock objects to represent the components of the Ably client library, and uses these to configure and create an instance of [DefaultAbly].
 * It exposes this instance as the [objectUnderTest] property, and also exposes the configured mocks so that each test case can perform verifications on them.
 */
class DefaultAblyTestEnvironment private constructor(
    /**
     * The [DefaultAbly] object created by [create].
     * It has been configured with an [AblySdkRealtimeFactory] mock that supplies [realtimeMock].
     */
    val objectUnderTest: DefaultAbly,
    /**
     * The [AblySdkRealtime] mock created by [create].
     * Its [AblySdkRealtime.channels] property returns [channelsMock].
     */
    val realtimeMock: AblySdkRealtime,
    /**
     * The [AblySdkRealtime.Channels] mock which [create] has created and configured [realtimeMock]’s [AblySdkRealtime.channels] property to return.
     * See the documentation for that method for details of how this mock is configured.
     */
    val channelsMock: AblySdkRealtime.Channels,
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
         * Its [AblySdkRealtime.Channel.presence] property returns [presenceMock].
         */
        val channelMock: AblySdkRealtime.Channel,
        /**
         * The [AblySdkRealtime.Presence] mock which [create] has created and configured [channelMock]’s [AblySdkRealtime.Channel.presence] property to return.
         */
        val presenceMock: AblySdkRealtime.Presence
    ) {
        /**
         * Mocks [presenceMock]’s [AblySdkRealtime.Presence.enter] method to immediately call its received completion listener’s [CompletionListener.onSuccess] method.
         */
        fun mockSuccessfulPresenceEnter() {
            val completionListenerSlot = slot<CompletionListener>()
            every {
                presenceMock.enter(
                    any(),
                    capture(completionListenerSlot)
                )
            } answers { completionListenerSlot.captured.onSuccess() }
        }
    }

    companion object {
        /**
         * Creates an instance of [DefaultAblyTestEnvironment], with some basic initial configuration applied to its mocks.
         *
         * Specifically:
         *
         * 1. It creates [numberOfTrackables] channel mocks, and configures each of their [AblySdkRealtime.Channel.state] properties to return [ChannelState.initialized].
         *    It exposes these channel mocks via the [configuredChannels] property, each with a different [ConfiguredChannel.name] value, each of which have a `"tracking:"` prefix.
         * 1. It creates a [AblySdkRealtime.Channels] mock, and configures it as follows:
         *    1. Its [AblySdkRealtime.Channels.containsKey] method returns `false` for all inputs;
         *    1. Its [AblySdkRealtime.Channels.get(channelName: String, channelOptions: ChannelOptions?)] method returns the mock from [configuredChannels] with the corresponding [ConfiguredChannel.channelName] property.
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

                val channelMock = mockk<AblySdkRealtime.Channel>()
                every { channelMock.state } returns ChannelState.initialized
                every { channelMock.presence } returns presenceMock

                ConfiguredChannel(trackableId, channelName, channelMock, presenceMock)
            }

            val channelsMock = mockk<AblySdkRealtime.Channels>()
            every { channelsMock.containsKey(any()) } returns false
            for (configuredChannel in configuredChannels) {
                every {
                    channelsMock.get(
                        configuredChannel.channelName,
                        any()
                    )
                } returns configuredChannel.channelMock
            }

            val realtimeMock = mockk<AblySdkRealtime>()
            every { realtimeMock.channels } returns channelsMock

            val factory = mockk<AblySdkRealtimeFactory>()
            every { factory.create(any()) } returns realtimeMock

            val connectionConfiguration =
                ConnectionConfiguration(Authentication.basic("", "")) // arbitrarily chosen
            val objectUnderTest =
                DefaultAbly(factory, connectionConfiguration, null /* arbitrarily chosen */)

            return DefaultAblyTestEnvironment(
                objectUnderTest,
                realtimeMock,
                channelsMock,
                configuredChannels
            )
        }
    }
}
