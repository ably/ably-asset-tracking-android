package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.mockConnectSuccess
import com.ably.tracking.test.common.mockDisconnectSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DefaultPublisherTest {
    private val ably = mockk<Ably>(relaxed = true)
    private val mapbox = mockk<Mapbox>(relaxed = true)
    private val resolutionPolicy = mockk<ResolutionPolicy>(relaxed = true)
    private val resolutionPolicyFactory = object : ResolutionPolicy.Factory {
        override fun createResolutionPolicy(hooks: ResolutionPolicy.Hooks, methods: ResolutionPolicy.Methods) =
            resolutionPolicy
    }

    @SuppressLint("MissingPermission")
    private val publisher: Publisher =
        DefaultPublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null)

    @Test(expected = ConnectionException::class)
    fun `should return an error when adding a trackable with subscribing to presence error`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        ably.mockConnectSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        runBlocking {
            publisher.add(Trackable(trackableId))
        }

        // then
    }

    @Test()
    fun `should disconnect from the channel when adding a trackable with subscribing to presence error`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        ably.mockConnectSuccess(trackableId)
        ably.mockDisconnectSuccess(trackableId)
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        runBlocking {
            try {
                publisher.add(Trackable(trackableId))
            } catch (exception: ConnectionException) {
                // ignoring exception in this test
            }
        }

        // then
        verify(exactly = 1) {
            ably.disconnect(trackableId, any(), any())
        }
    }

    @Test()
    fun `should not repeat adding process when adding a trackable that is already added`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockConnectSuccess(trackableId)

        // when
        runBlocking {
            publisher.add(trackable)
            publisher.add(trackable)
        }

        // then
        verify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any(), any())
        }
    }

    @Test()
    fun `should repeat adding process when adding the first trackable has failed before starting to add the second one`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockConnectFailureThenSuccess(trackableId)

        // when
        runBlocking {
            try {
                publisher.add(trackable)
            } catch (exception: Exception) {
                // ignoring exception in this test
            }
            publisher.add(trackable)
        }

        // then
        verify(exactly = 2) {
            ably.connect(trackableId, any(), any(), any(), any(), any())
        }
    }
}
