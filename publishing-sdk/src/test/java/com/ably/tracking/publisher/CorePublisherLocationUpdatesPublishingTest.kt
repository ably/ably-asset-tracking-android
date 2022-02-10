package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.Accuracy
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.test.common.createLocation
import com.ably.tracking.test.common.mockCreateConnectionSuccess
import com.ably.tracking.test.common.mockSendEnhancedLocationFailure
import com.ably.tracking.test.common.mockSendEnhancedLocationFailureThenSuccess
import com.ably.tracking.test.common.mockSendEnhancedLocationSuccess
import com.ably.tracking.test.common.mockSendRawLocationSuccess
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CorePublisherLocationUpdatesPublishingTest {
    private val ably = mockk<Ably>(relaxed = true)
    private val mapbox = mockk<Mapbox>(relaxed = true)
    private val resolutionPolicy = mockk<ResolutionPolicy>(relaxed = true)
    private val resolutionPolicyFactory = object : ResolutionPolicy.Factory {
        override fun createResolutionPolicy(hooks: ResolutionPolicy.Hooks, methods: ResolutionPolicy.Methods) =
            resolutionPolicy
    }

    @SuppressLint("MissingPermission")
    private val corePublisher: CorePublisher =
        createCorePublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, false, false, null)

    @Test
    fun `Should send a message only once if publishing it succeeds`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
        addTrackable(Trackable(trackableId))
        ably.mockSendEnhancedLocationSuccess(trackableId)

        // when
        corePublisher.enqueue(createEnhancedLocationChangedEvent(createLocation()))

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher()
        }
        verify(exactly = 1) {
            ably.sendEnhancedLocation(trackableId, any(), any())
        }
    }

    @Test
    fun `Should try to resend a message if it fails for the first time`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
        addTrackable(Trackable(trackableId))
        ably.mockSendEnhancedLocationFailureThenSuccess(trackableId)

        // when
        corePublisher.enqueue(createEnhancedLocationChangedEvent(createLocation()))

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher()
        }
        verify(exactly = 2) {
            ably.sendEnhancedLocation(trackableId, any(), any())
        }
    }

    @Test
    fun `Should not try to resend a message if it fails for the second time`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
        addTrackable(Trackable(trackableId))
        ably.mockSendEnhancedLocationFailure(trackableId)

        // when
        corePublisher.enqueue(createEnhancedLocationChangedEvent(createLocation()))

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher()
        }
        verify(exactly = 2) {
            ably.sendEnhancedLocation(trackableId, any(), any())
        }
    }

    @Test
    fun `Should not send raw messages if they are disabled`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
        addTrackable(Trackable(trackableId))
        ably.mockSendRawLocationSuccess(trackableId)

        // when
        corePublisher.enqueue(createRawLocationChangedEvent(createLocation()))

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher()
        }
        verify(exactly = 0) {
            ably.sendRawLocation(trackableId, any(), any())
        }
    }

    @SuppressLint("MissingPermission")
    @Test
    fun `Should send raw messages if they are enabled`() {
        // given
        val corePublisher =
            createCorePublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, true, false, null)
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
        addTrackable(Trackable(trackableId), corePublisher)
        ably.mockSendRawLocationSuccess(trackableId)

        // when
        corePublisher.enqueue(createRawLocationChangedEvent(createLocation()))

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher(corePublisher)
        }
        verify(exactly = 1) {
            ably.sendRawLocation(trackableId, any(), any())
        }
    }

    private fun createEnhancedLocationChangedEvent(location: Location) =
        EnhancedLocationChangedEvent(location, emptyList(), LocationUpdateType.ACTUAL)

    private fun createRawLocationChangedEvent(location: Location) =
        RawLocationChangedEvent(location)

    private fun mockAllTrackablesResolution(resolution: Resolution) {
        every { resolutionPolicy.resolve(any<TrackableResolutionRequest>()) } returns resolution
    }

    private fun addTrackable(
        trackable: Trackable,
        corePublisher: CorePublisher = this.corePublisher
    ) {
        ably.mockCreateConnectionSuccess(trackable.id)
        runBlocking(Dispatchers.IO) {
            addTrackableToCorePublisher(trackable, corePublisher)
        }
    }

    private suspend fun addTrackableToCorePublisher(
        trackable: Trackable,
        corePublisher: CorePublisher = this.corePublisher
    ): StateFlow<TrackableState> {
        return suspendCoroutine { continuation ->
            corePublisher.request(
                AddTrackableEvent(trackable) {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private suspend fun stopCorePublisher(corePublisher: CorePublisher = this.corePublisher) {
        suspendCoroutine<Unit> { continuation ->
            corePublisher.request(
                StopEvent {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}
