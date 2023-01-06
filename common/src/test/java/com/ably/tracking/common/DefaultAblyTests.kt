package com.ably.tracking.common

import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DefaultAblyTests {
    // This is just an example test to check that the AblySdkRealtime mocks are working correctly. We need to add a full set of unit tests for DefaultAbly; see https://github.com/ably/ably-asset-tracking-android/issues/869
    @Test
    fun `connect fetches the channel and then enters presence on it, and when that succeeds the call to connect succeeds`() {
        val presence = mockk<AblySdkRealtime.Presence>()
        val completionListenerSlot = slot<CompletionListener>()
        every { presence.enter(any(), capture(completionListenerSlot)) } answers { completionListenerSlot.captured.onSuccess() }

        val channel = mockk<AblySdkRealtime.Channel>()
        every { channel.state } returns ChannelState.initialized
        every { channel.presence } returns presence

        val channels = mockk<AblySdkRealtime.Channels>()
        every { channels.containsKey(any()) } returns false
        every { channels.get("tracking:someTrackableId", any()) } returns channel

        val ably = mockk<AblySdkRealtime>()
        every { ably.channels } returns channels

        val factory = mockk<AblySdkRealtimeFactory>()
        every { factory.create(any()) } returns ably

        val configuration = ConnectionConfiguration(Authentication.basic("someClientId", "someApiKey"))
        val client = DefaultAbly(factory, configuration, null)

        runBlocking {
            client.connect("someTrackableId", PresenceData(""))
        }

        verify {
            channels.get("tracking:someTrackableId", any())
            presence.enter(any(), any())
        }
    }
}
