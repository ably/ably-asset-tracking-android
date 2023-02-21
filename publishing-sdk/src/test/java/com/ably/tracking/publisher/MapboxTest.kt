package com.ably.tracking.publisher

import com.ably.tracking.Location
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class MapboxTest {

    private val mockLocationUpdatesObserver = mockk<LocationUpdatesObserver> {
        every { onRawLocationChanged(any()) } just runs
        every { onEnhancedLocationChanged(any(), any()) } just runs
    }

    private val timeProvider: TimeProvider = mockk()

    private val mapboxLocationObserverProvider =
        MapboxLocationObserverProvider(null, timeProvider, "MapboxTest")

    private val mapboxLocationObserver =
        mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)

    @Test
    fun `Should forward valid raw location update`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1
            ),
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1L
            )
        )

    @Test
    fun `Should forward raw location update with repaired accuracy, bearing and speed`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = Float.NaN,
                bearing = Float.NaN,
                speed = Float.NaN,
                time = 1
            ),
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = -1.0f,
                bearing = -1.0f,
                speed = -1.0f,
                time = 1L
            )
        )

    @Test
    fun `Should forward raw location update with no accuracy, no bearing and no speed`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = null,
                bearing = null,
                speed = null,
                time = 1
            ),
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = -1.0f,
                bearing = -1.0f,
                speed = -1.0f,
                time = 1L
            )
        )

    @Test
    fun `Should suppress raw location update with invalid latitude`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = Double.NaN,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1
            ),
            expectedLocation = null
        )

    @Test
    fun `Should suppress raw location update with invalid longitude`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = Double.NaN,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1
            ),
            expectedLocation = null
        )

    @Test
    fun `Should suppress raw location update with invalid altitude`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = 1.0,
                altitude = Double.NaN,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1
            ),
            expectedLocation = null
        )

    @Test
    fun `Should suppress raw location update with zero time`() =
        testRawLocationUpdate(
            inputLocation = createAndroidLocation(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 0
            ),
            expectedLocation = null
        )

    @Test
    fun `Should forward valid enhanced location update`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1L
            ),
            expectedIntermediateLocations = emptyList()
        )

    @Test
    fun `Should forward enhanced location update with repaired accuracy, bearing and speed`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = Float.NaN,
                    bearing = Float.NaN,
                    speed = Float.NaN,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = -1.0f,
                bearing = -1.0f,
                speed = -1.0f,
                time = 1L
            ),
            expectedIntermediateLocations = emptyList()
        )

    @Test
    fun `Should forward enhanced location update with no accuracy, no bearing and no speed`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = null,
                    bearing = null,
                    speed = null,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = -1.0f,
                bearing = -1.0f,
                speed = -1.0f,
                time = 1L
            ),
            expectedIntermediateLocations = emptyList()
        )

    @Test
    fun `Should suppress enhanced location update with invalid latitude`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = Double.NaN,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = null,
            expectedIntermediateLocations = null
        )

    @Test
    fun `Should suppress enhanced location update with invalid longitude`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = Double.NaN,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = null,
            expectedIntermediateLocations = null
        )

    @Test
    fun `Should suppress enhanced location update with invalid altitude`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = Double.NaN,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = null,
            expectedIntermediateLocations = null
        )

    @Test
    fun `Should suppress enhanced location update with no altitude`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = null,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 1
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = null,
            expectedIntermediateLocations = null
        )

    @Test
    fun `Should not suppress enhanced location update with zero time`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 0
                ),
                keyPoints = emptyList(),
            ),
            currentTime = 1L,
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1L
            ),
            expectedIntermediateLocations = emptyList()
        )

    @Test
    fun `Should forward valid intermediate location updates`() =
        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 0
                ),
                keyPoints = listOf(
                    createAndroidLocation(1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 1),
                    createAndroidLocation(2.0, 2.0, 2.0, 2.0f, 2.0f, 2.0f, 2),
                    // the last key point is always the same as the enhanced location
                    // https://docs.mapbox.com/android/navigation/api/2.8.0/libnavigation-core/com.mapbox.navigation.core.trip.session/-location-matcher-result/key-points.html
                    createAndroidLocation(3.0, 3.0, 3.0, 3.0f, 3.0f, 3.0f, 3),
                ),
            ),
            currentTime = 1L,
            expectedLocation = Location(
                latitude = 1.0,
                longitude = 1.0,
                altitude = 1.0,
                accuracy = 1.0f,
                bearing = 1.0f,
                speed = 1.0f,
                time = 1L
            ),
            expectedIntermediateLocations = listOf(
                Location(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 2L
                ),
                Location(
                    latitude = 2.0,
                    longitude = 2.0,
                    altitude = 2.0,
                    accuracy = 2.0f,
                    bearing = 2.0f,
                    speed = 2.0f,
                    time = 3L
                )
            )
        )

    @Test
    fun `Should suppress invalid intermediate location updates and forward others with repaired accuracy, bearing and speed`() =

        testEnhancedLocationUpdate(
            inputLocation = createMapboxLocationMatcherResult(
                enhancedLocation = createAndroidLocation(
                    latitude = 6.0,
                    longitude = 6.0,
                    altitude = 6.0,
                    accuracy = 6.0f,
                    bearing = 6.0f,
                    speed = 6.0f,
                    time = 6
                ),
                keyPoints = listOf(
                    createAndroidLocation(
                        latitude = 1.0,
                        longitude = 1.0,
                        altitude = 1.0,
                        accuracy = 1.0f,
                        bearing = 1.0f,
                        speed = 1.0f,
                        time = 1
                    ),
                    createAndroidLocation(
                        latitude = Double.NaN,
                        longitude = 2.0,
                        altitude = 2.0,
                        accuracy = 2.0f,
                        bearing = 2.0f,
                        speed = 2.0f,
                        time = 2
                    ), // invalid, should be suppressed
                    createAndroidLocation(
                        latitude = 3.0,
                        longitude = Double.NaN,
                        altitude = 3.0,
                        accuracy = 3.0f,
                        bearing = 3.0f,
                        speed = 3.0f,
                        time = 3
                    ), // invalid, should be suppressed
                    createAndroidLocation(
                        latitude = 4.0,
                        longitude = 4.0,
                        altitude = Double.NaN,
                        accuracy = 4.0f,
                        bearing = 4.0f,
                        speed = 4.0f,
                        time = 4
                    ), // invalid, should be suppressed
                    createAndroidLocation(
                        latitude = 5.0,
                        longitude = 5.0,
                        altitude = 5.0,
                        accuracy = Float.NaN,
                        bearing = Float.NaN,
                        speed = Float.NaN,
                        time = 5
                    ), // should be repaired
                    // the last key point is always the same as the enhanced location
                    // https://docs.mapbox.com/android/navigation/api/2.8.0/libnavigation-core/com.mapbox.navigation.core.trip.session/-location-matcher-result/key-points.html
                    createAndroidLocation(
                        latitude = 6.0,
                        longitude = 6.0,
                        altitude = 6.0,
                        accuracy = 6.0f,
                        bearing = 6.0f,
                        speed = 6.0f,
                        time = 6
                    ),
                ),
            ),
            currentTime = 10L,
            expectedLocation = Location(
                latitude = 6.0,
                longitude = 6.0,
                altitude = 6.0,
                accuracy = 6.0f,
                bearing = 6.0f,
                speed = 6.0f,
                time = 10L
            ),
            expectedIntermediateLocations = listOf(
                Location(
                    latitude = 1.0,
                    longitude = 1.0,
                    altitude = 1.0,
                    accuracy = 1.0f,
                    bearing = 1.0f,
                    speed = 1.0f,
                    time = 5L
                ),
                Location(
                    latitude = 5.0,
                    longitude = 5.0,
                    altitude = 5.0,
                    accuracy = -1.0f,
                    bearing = -1.0f,
                    speed = -1.0f,
                    time = 9L
                )
            )
        )

    private fun testRawLocationUpdate(
        inputLocation: android.location.Location,
        expectedLocation: Location?
    ) {
        // given
        // when
        mapboxLocationObserver.onNewRawLocation(inputLocation)

        // then
        if (expectedLocation == null) {
            verify(exactly = 0) { mockLocationUpdatesObserver.onRawLocationChanged(any()) }
        } else {
            verify { mockLocationUpdatesObserver.onRawLocationChanged(expectedLocation) }
        }
    }

    private fun testEnhancedLocationUpdate(
        inputLocation: LocationMatcherResult,
        currentTime: Long,
        expectedLocation: Location?,
        expectedIntermediateLocations: List<Location>?
    ) {
        // given
        timeProvider.mockTime(currentTime)

        // when
        mapboxLocationObserver.onNewLocationMatcherResult(inputLocation)

        // then
        when {
            expectedLocation == null && expectedIntermediateLocations == null -> {
                verify(exactly = 0) {
                    mockLocationUpdatesObserver.onEnhancedLocationChanged(
                        any(),
                        any()
                    )
                }
            }
            expectedLocation != null && expectedIntermediateLocations != null -> {
                verify {
                    mockLocationUpdatesObserver.onEnhancedLocationChanged(
                        expectedLocation,
                        expectedIntermediateLocations
                    )
                }
            }
            else ->
                Assert.fail("both expected values should either be null or non-null, got $expectedLocation, $expectedIntermediateLocations")
        }
    }

    private fun createAndroidLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double?,
        accuracy: Float?,
        bearing: Float?,
        speed: Float?,
        time: Long,
    ): android.location.Location {
        val location = mockk<android.location.Location>()
        every { location.latitude } returns latitude
        every { location.longitude } returns longitude
        every { location.altitude } returns (altitude ?: Double.MIN_VALUE)
        every { location.hasAltitude() } returns (altitude != null)
        every { location.accuracy } returns (accuracy ?: Float.MIN_VALUE)
        every { location.hasAccuracy() } returns (accuracy != null)
        every { location.bearing } returns (bearing ?: Float.MIN_VALUE)
        every { location.hasBearing() } returns (bearing != null)
        every { location.speed } returns (speed ?: Float.MIN_VALUE)
        every { location.hasSpeed() } returns (speed != null)
        every { location.time } returns time
        return location
    }

    private fun createMapboxLocationMatcherResult(
        enhancedLocation: android.location.Location,
        keyPoints: List<android.location.Location>,
    ): LocationMatcherResult {
        val locationMatcherResult = mockk<LocationMatcherResult>()
        every { locationMatcherResult.enhancedLocation } returns enhancedLocation
        every { locationMatcherResult.keyPoints } returns keyPoints
        return locationMatcherResult
    }

    private fun TimeProvider.mockTime(time: Long) {
        every { getCurrentTime() } returns time
    }
}
