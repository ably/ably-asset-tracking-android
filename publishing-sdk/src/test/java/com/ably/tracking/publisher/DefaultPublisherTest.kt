package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.ConnectionException
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.mockConnectFailureThenSuccess
import com.ably.tracking.test.common.mockConnectSuccess
import com.ably.tracking.test.common.mockSubscribeToPresenceError
import com.ably.tracking.test.common.mockSubscribeToPresenceSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.TimeoutCancellationException
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        DefaultPublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, false, false, null)

    @Test()
    fun `should not return an error when adding a trackable with subscribing to presence error`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        ably.mockConnectSuccess(trackableId)
        ably.mockSubscribeToPresenceError(trackableId)

        // when
        runBlocking {
            publisher.add(Trackable(trackableId))
        }

        // then
    }

    @Test()
    fun `should not disconnect from the channel when adding a trackable with subscribing to presence error`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        ably.mockConnectSuccess(trackableId)
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
        coVerify(exactly = 0) {
            ably.disconnect(trackableId, any())
        }
    }

    @Test()
    fun `should not repeat adding process when adding a trackable that is already added`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockConnectSuccess(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        runBlocking {
            publisher.add(trackable)
            publisher.add(trackable)
        }

        // then
        coVerify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any())
        }
    }

    @Test()
    fun `should repeat adding process when adding the first trackable has failed with fatal exception before starting to add the second one`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockConnectFailureThenSuccess(trackableId, isFatal = true)
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
        coVerify(exactly = 2) {
            ably.connect(trackableId, any(), any(), any(), any())
        }
    }

    @Test()
    fun `should not repeat adding process when adding a trackable that is currently being added`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        ably.mockConnectSuccess(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

        // when
        runBlocking {
            coroutineScope {
                launch { publisher.add(trackable) }
                launch { publisher.add(trackable) }
            }
        }

        // then
        coVerify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any())
        }
    }

    @Test()
    fun `should fail adding process when adding a trackable that is currently being added and it fails with fatal exception`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        var didFirstAddFail = false
        var didSecondAddFail = false
        // without the callback delay sometimes the first add() ends before the second one begins
        ably.mockConnectFailureThenSuccess(trackableId, isFatal = true, callbackDelayInMilliseconds = 100L)
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
        coVerify(exactly = 1) {
            ably.connect(trackableId, any(), any(), any(), any())
        }
    }

    @Test()
    fun `should not finish second add call before the first one completes`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        val trackable = Trackable(trackableId)
        val callsOrder = mutableListOf<Int>()
        ably.mockConnectSuccess(trackableId)
        ably.mockSubscribeToPresenceSuccess(trackableId)

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

    @Test()
    fun `should clear the route if the new active trackable does not have a destination`() {
        // given
        val trackableWithoutDestination = Trackable(UUID.randomUUID().toString())
        ably.mockConnectSuccess(trackableWithoutDestination.id)
        ably.mockSubscribeToPresenceSuccess(trackableWithoutDestination.id)

        // when
        runBlocking {
            publisher.track(trackableWithoutDestination)
        }

        // then
        verify(exactly = 1) {
            mapbox.clearRoute()
        }
    }

    @Test()
    fun `close - behaviour when wrapped in a withTimeout block, when the timeout elapses during the presence leave operation`() {
        // Given...
        // ...when ably.close() is called, it takes 2 seconds to complete...
        coEvery { ably.close(any()) } coAnswers { delay(2000) }

        // When...
        var caughtTimeoutCancellationException = false
        runBlocking {
            try {
                // ...we call publisher.stop() from within a withTimeout block with a timeout of 1 second...
                withTimeout(1000) {
                    publisher.stop()
                }
            } catch (e: TimeoutCancellationException) {
                caughtTimeoutCancellationException = true
            }
        }

        // Then...
        coVerify(exactly = 1) {
            // ...ably.close() is called...
            ably.close(any())
        }

        // ...and the withTimeout block throws a TimeoutCancellationException.
        Assert.assertTrue(caughtTimeoutCancellationException)

        // This test exists for similar probably-paranoid-and-misguided reasons as the test "close - behaviour when wrapped in a withTimeout block, when the timeout elapses during the presence leave operation" in DefaultAblyTests - to give me confidence that if the publisher.close() call is wrapped in a withTimeout block and the timeout elapses, then the TimeoutCancellationException will find its way to the caller of withTimeout.
    }
}
