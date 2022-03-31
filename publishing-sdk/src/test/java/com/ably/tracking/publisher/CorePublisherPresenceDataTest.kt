package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceData
import com.ably.tracking.test.common.mockCreateSuspendingConnectionSuccess
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

@SuppressLint("MissingPermission")
class CorePublisherPresenceDataTest {
    private val ably = mockk<Ably>(relaxed = true)
    private val mapbox = mockk<Mapbox>(relaxed = true)
    private val resolutionPolicy = mockk<ResolutionPolicy>(relaxed = true)
    private val resolutionPolicyFactory = object : ResolutionPolicy.Factory {
        override fun createResolutionPolicy(hooks: ResolutionPolicy.Hooks, methods: ResolutionPolicy.Methods) =
            resolutionPolicy
    }

    @Before
    fun setup() {
        mockAllTrackablesResolution(Resolution(Accuracy.MAXIMUM, 0, 0.0))
    }

    @Test
    fun `Should se rawMessages to null in the presence data if they are disabled`() {
        val corePublisher: CorePublisher =
            createCorePublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, null, false, null)
        // given
        val trackableId = UUID.randomUUID().toString()

        // when
        addTrackable(Trackable(trackableId), corePublisher)

        // then
        runBlocking {
            stopCorePublisher(corePublisher)
        }
        val expectedPresenceData = PresenceData(ClientTypes.PUBLISHER, null, null)
        coVerify(exactly = 1) {
            ably.connect(trackableId, expectedPresenceData, any(), any(), any())
        }
    }

    @Test
    fun `Should set rawMessages to true in the presence data if they are enabled`() {
        val corePublisher: CorePublisher =
            createCorePublisher(ably, mapbox, resolutionPolicyFactory, RoutingProfile.DRIVING, null, true, false, null)
        // given
        val trackableId = UUID.randomUUID().toString()

        // when
        addTrackable(Trackable(trackableId), corePublisher)

        // then
        runBlocking {
            stopCorePublisher(corePublisher)
        }
        val expectedPresenceData = PresenceData(ClientTypes.PUBLISHER, null, true)
        coVerify(exactly = 1) {
            ably.connect(trackableId, expectedPresenceData, any(), any(), any())
        }
    }

    private fun mockAllTrackablesResolution(resolution: Resolution) {
        every { resolutionPolicy.resolve(any<TrackableResolutionRequest>()) } returns resolution
    }

    private fun addTrackable(trackable: Trackable, corePublisher: CorePublisher) {
        ably.mockCreateSuspendingConnectionSuccess(trackable.id)
        runBlocking(Dispatchers.IO) {
            addTrackableToCorePublisher(trackable, corePublisher)
        }
    }

    private suspend fun addTrackableToCorePublisher(
        trackable: Trackable,
        corePublisher: CorePublisher
    ): StateFlow<TrackableState> {
        return suspendCoroutine { continuation ->
            corePublisher.addTrackable(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    private suspend fun stopCorePublisher(corePublisher: CorePublisher) {
        suspendCoroutine<Unit> { continuation ->
            corePublisher.stop {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}
