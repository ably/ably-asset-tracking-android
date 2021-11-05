package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.Accuracy
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.locationprovider.LocationProvider
import com.ably.tracking.locationprovider.RoutingProfile
import com.ably.tracking.test.common.createLocation
import com.ably.tracking.test.common.mockCreateConnectionSuccess
import com.ably.tracking.test.common.mockSendEnhancedLocationSuccess
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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CorePublisherResolutionTest(
    private val desiredInterval: Int,
    private val expectedNumberOfSentMessages: Int,
    private val numberOfLocationUpdates: Int,
    private val intervalBetweenLocationUpdates: Int,
    private val minimumDisplacement: Double,
    private val distanceBetweenLocationUpdates: Double,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "Location updates {2} | Expected messages {1} | " +
                "Desired interval {0} | Interval between location updates {3} | " +
                "Minimum displacement {4} | Distance between location updates {5}"
        )
        fun data() = listOf(
            params(
                desiredInterval = 1_000,
                intervalBetweenLocationUpdates = 1_000,
                numberOfLocationUpdates = 3,
                expectedNumberOfSentMessages = 3
            ),
            params(
                desiredInterval = 2_000,
                intervalBetweenLocationUpdates = 1_000,
                numberOfLocationUpdates = 3,
                expectedNumberOfSentMessages = 2
            ),
            params(
                desiredInterval = 10_000,
                intervalBetweenLocationUpdates = 1_000,
                numberOfLocationUpdates = 30,
                expectedNumberOfSentMessages = 3
            ),
            params(
                minimumDisplacement = 1.0,
                distanceBetweenLocationUpdates = 1.0,
                numberOfLocationUpdates = 3,
                expectedNumberOfSentMessages = 3
            ),
            params(
                minimumDisplacement = 2.0,
                distanceBetweenLocationUpdates = 1.0,
                numberOfLocationUpdates = 3,
                expectedNumberOfSentMessages = 2
            ),
            params(
                minimumDisplacement = 3.0,
                distanceBetweenLocationUpdates = 1.0,
                numberOfLocationUpdates = 3,
                expectedNumberOfSentMessages = 1
            ),
        )

        private fun params(
            numberOfLocationUpdates: Int,
            expectedNumberOfSentMessages: Int,
            desiredInterval: Int = 1000,
            intervalBetweenLocationUpdates: Int = 0, // by default temporal resolution won't have any effect
            minimumDisplacement: Double = 1.0,
            distanceBetweenLocationUpdates: Double = 0.0, // by default spatial resolution won't have any effect
        ) = arrayOf(
            desiredInterval,
            expectedNumberOfSentMessages,
            numberOfLocationUpdates,
            intervalBetweenLocationUpdates,
            minimumDisplacement,
            distanceBetweenLocationUpdates
        )
    }

    private val ably = mockk<Ably>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>(relaxed = true)
    private val resolutionPolicy = mockk<ResolutionPolicy>(relaxed = true)
    private val resolutionPolicyFactory = object : ResolutionPolicy.Factory {
        override fun createResolutionPolicy(hooks: ResolutionPolicy.Hooks, methods: ResolutionPolicy.Methods) =
            resolutionPolicy
    }

    @SuppressLint("MissingPermission")
    private val corePublisher: CorePublisher =
        createCorePublisher(ably, locationProvider, resolutionPolicyFactory, RoutingProfile.DRIVING, null, false)

    @Test
    fun `Should send limited location updates`() {
        // given
        val trackableId = UUID.randomUUID().toString()
        mockAllTrackablesResolution(Resolution(Accuracy.BALANCED, desiredInterval.toLong(), minimumDisplacement))
        addTrackable(Trackable(trackableId))
        var locationTimestamp = 1000L
        var location: Location? = null
        ably.mockSendEnhancedLocationSuccess(trackableId)

        // when
        repeat(numberOfLocationUpdates) {
            val nextLocation = createNextPublisherLocation(location, distanceBetweenLocationUpdates, locationTimestamp)
            corePublisher.enqueue(createEnhancedLocationChangedEvent(nextLocation))
            location = nextLocation
            locationTimestamp += intervalBetweenLocationUpdates
        }

        // then
        runBlocking {
            delay(500) // we're assuming that within this time all events will be processed or at least placed in the queue in the final order
            stopCorePublisher()
        }
        verify(exactly = expectedNumberOfSentMessages) {
            ably.sendEnhancedLocation(trackableId, any(), any())
        }
    }

    private fun createEnhancedLocationChangedEvent(location: Location) =
        EnhancedLocationChangedEvent(location, emptyList(), LocationUpdateType.ACTUAL)

    private fun mockAllTrackablesResolution(resolution: Resolution) {
        every { resolutionPolicy.resolve(any<TrackableResolutionRequest>()) } returns resolution
    }

    private fun addTrackable(trackable: Trackable) {
        ably.mockCreateConnectionSuccess(trackable.id)
        runBlocking(Dispatchers.IO) {
            addTrackableToCorePublisher(trackable)
        }
    }

    private suspend fun addTrackableToCorePublisher(trackable: Trackable): StateFlow<TrackableState> {
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

    private suspend fun stopCorePublisher() {
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

    private fun createNextPublisherLocation(oldLocation: Location?, distanceInMeters: Double, timestamp: Long) =
        when {
            oldLocation != null && distanceInMeters > 0.0 -> createDistantLocation(
                oldLocation,
                distanceInMeters,
                timestamp
            )
            oldLocation != null && distanceInMeters == 0.0 -> createLocation(
                oldLocation.latitude,
                oldLocation.longitude,
                timestamp
            )
            else -> createLocation(0.0, 0.0, timestamp) // We start at 0.0 because it's easier to mock distant locations
        }

    /**
     * Creates a location that has the same latitude as the [oldLocation] but its longitude is changed
     * so it is [distanceInMeters] away from the [oldLocation].
     */
    private fun createDistantLocation(oldLocation: Location, distanceInMeters: Double, timestamp: Long): Location {
        // If you add this value to the longitude it will move the location 1 meter away
        // (precisely 1.0007622222311054 meters away). This approximation will work well
        // as long as the [distanceInMeters] is smaller than 1310.
        val oneMeterDistanceOnLongitude = 0.00000899
        return createLocation(
            oldLocation.latitude,
            oldLocation.longitude + distanceInMeters * oneMeterDistanceOnLongitude,
            timestamp
        )
    }
}
