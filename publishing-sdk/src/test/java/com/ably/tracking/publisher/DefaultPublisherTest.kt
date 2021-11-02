package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.mockConnectFailureThenSuccess
import com.ably.tracking.test.common.mockConnectSuccess
import com.ably.tracking.test.common.mockCreateConnectionSuccess
import com.ably.tracking.test.common.mockDisconnectSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
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
        DefaultPublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, false)

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
        ably.mockCreateConnectionSuccess(trackableId)

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
        ably.mockSubscribeToPresenceSuccess(trackableId)

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

    @Test()
    fun `should not repeat adding process when adding a trackable that is currently being added`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockCreateConnectionSuccess(trackableId)

        // when
        runBlocking {
            coroutineScope {
                launch { publisher.add(trackable) }
                launch { publisher.add(trackable) }
            }
        }

        // then
        verify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any(), any())
        }
    }

    @Test()
    fun `should fail adding process when adding a trackable that is currently being added and it fails`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        var didFirstAddFail = false
        var didSecondAddFail = false
        // without the callback delay sometimes the first add() ends before the second one begins
        ably.mockConnectFailureThenSuccess(trackableId, callbackDelayInMilliseconds = 100L)
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        runBlocking {
            coroutineScope {
                launch {
                    try {
                        publisher.add(trackable)
                    } catch (exception: Exception) {
                        didFirstAddFail = true
                    }
                }
                launch {
                    try {
                        publisher.add(trackable)
                    } catch (exception: Exception) {
                        didSecondAddFail = true
                    }
                }
            }
        }

        // then
        Assert.assertTrue("First add should fail", didFirstAddFail)
        Assert.assertTrue("Second add should fail", didSecondAddFail)
        verify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any(), any())
        }
    }

    @Test()
    fun `should not finish second add call before the first one completes`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        val callsOrder = mutableListOf<Int>()
        ably.mockCreateConnectionSuccess(trackableId)

        // when
        runBlocking {
            coroutineScope {
                launch {
                    publisher.add(trackable)
                    callsOrder.add(1)
                }
                launch {
                    publisher.add(trackable)
                    callsOrder.add(2)
                }
            }
        }

        // then
        Assert.assertEquals(2, callsOrder.size)
        Assert.assertEquals(1, callsOrder[0])
        Assert.assertEquals(2, callsOrder[1])
    }
}
