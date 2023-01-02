package com.ably.tracking.common

import com.ably.tracking.common.helper.DefaultAblyTestEnvironment
import io.mockk.verify
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
}
