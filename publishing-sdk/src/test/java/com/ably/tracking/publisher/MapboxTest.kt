package com.ably.tracking.publisher

import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class MapboxTest {
    private val mockLocationUpdatesObserver = mockk<LocationUpdatesObserver>()
    private val mapboxLocationObserverProvider =
        MapboxLocationObserverProvider(null, "MapboxTest")

    @Test
    fun `Should forward valid raw location update`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    1.0,
                    1.0,
                    1.0,
                    1.0f,
                    1.0f,
                    1.0f,
                    1
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onRawLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 1.0)
                    Assert.assertTrue(it.longitude == 1.0)
                    Assert.assertTrue(it.altitude == 1.0)
                    Assert.assertTrue(it.accuracy == 1.0f)
                    Assert.assertTrue(it.bearing == 1.0f)
                    Assert.assertTrue(it.speed == 1.0f)
                    Assert.assertTrue(it.time == 1L)
                }
            )
        }
    }

    @Test
    fun `Should forward raw location update with repaired accuracy, bearing and speed`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    1.0,
                    1.0,
                    1.0,
                    Float.NaN,
                    Float.NaN,
                    Float.NaN,
                    1
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onRawLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 1.0)
                    Assert.assertTrue(it.longitude == 1.0)
                    Assert.assertTrue(it.altitude == 1.0)
                    Assert.assertTrue(it.accuracy == -1.0f)
                    Assert.assertTrue(it.bearing == -1.0f)
                    Assert.assertTrue(it.speed == -1.0f)
                    Assert.assertTrue(it.time == 1L)
                }
            )
        }
    }

    @Test
    fun `Should suppress raw location update with invalid latitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    Double.NaN,
                    1.0,
                    1.0,
                    1.0f,
                    1.0f,
                    1.0f,
                    1
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onRawLocationChanged(any())
        }
    }

    @Test
    fun `Should suppress raw location update with invalid longitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    1.0,
                    Double.NaN,
                    1.0,
                    1.0f,
                    1.0f,
                    1.0f,
                    1
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onRawLocationChanged(any())
        }
    }

    @Test
    fun `Should suppress raw location update with invalid altitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    1.0,
                    1.0,
                    Double.NaN,
                    1.0f,
                    1.0f,
                    1.0f,
                    1
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onRawLocationChanged(any())
        }
    }

    @Test
    fun `Should suppress raw location update with zero time`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every { mockLocationUpdatesObserver.onRawLocationChanged(any()) } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewRawLocation(
                createAndroidLocation(
                    1.0,
                    1.0,
                    1.0,
                    1.0f,
                    1.0f,
                    1.0f,
                    0
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onRawLocationChanged(any())
        }
    }

    @Test
    fun `Should forward valid enhanced location update`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 1),
                    emptyList(),
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 1.0)
                    Assert.assertTrue(it.longitude == 1.0)
                    Assert.assertTrue(it.altitude == 1.0)
                    Assert.assertTrue(it.accuracy == 1.0f)
                    Assert.assertTrue(it.bearing == 1.0f)
                    Assert.assertTrue(it.speed == 1.0f)
                    Assert.assertTrue(it.time > 1) // should now be current time
                },
                withArg {
                    Assert.assertTrue(it.isEmpty())
                }
            )
        }
    }

    @Test
    fun `Should forward enhanced location update with repaired accuracy, bearing and speed`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(1.0, 1.0, 1.0, Float.NaN, Float.NaN, Float.NaN, 1),
                    emptyList(),
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 1.0)
                    Assert.assertTrue(it.longitude == 1.0)
                    Assert.assertTrue(it.altitude == 1.0)
                    Assert.assertTrue(it.accuracy == -1.0f)
                    Assert.assertTrue(it.bearing == -1.0f)
                    Assert.assertTrue(it.speed == -1.0f)
                    Assert.assertTrue(it.time > 1) // should now be current time
                },
                withArg {
                    Assert.assertTrue(it.isEmpty())
                }
            )
        }
    }

    @Test
    fun `Should suppress enhanced location update with invalid latitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(Double.NaN, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 1),
                    emptyList(),
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(any(), any())
        }
    }

    @Test
    fun `Should suppress enhanced location update with invalid longitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(1.0, Double.NaN, 1.0, 1.0f, 1.0f, 1.0f, 1),
                    emptyList(),
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(any(), any())
        }
    }

    @Test
    fun `Should suppress enhanced location update with invalid altitude`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(1.0, 1.0, Double.NaN, 1.0f, 1.0f, 1.0f, 1),
                    emptyList(),
                )
            )
        }
        verify(exactly = 0) {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(any(), any())
        }
    }

    @Test
    fun `Should not suppress enhanced location update with zero time`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 0),
                    emptyList(),
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 1.0)
                    Assert.assertTrue(it.longitude == 1.0)
                    Assert.assertTrue(it.altitude == 1.0)
                    Assert.assertTrue(it.accuracy == 1.0f)
                    Assert.assertTrue(it.bearing == 1.0f)
                    Assert.assertTrue(it.speed == 1.0f)
                    Assert.assertTrue(it.time > 1) // should now be current time
                },
                withArg {
                    Assert.assertTrue(it.isEmpty())
                }
            )
        }
    }

    @Test
    fun `Should forward valid intermediate location updates`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(3.0, 3.0, 3.0, 3.0f, 3.0f, 3.0f, 3),
                    listOf(
                        createAndroidLocation(1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 1),
                        createAndroidLocation(2.0, 2.0, 2.0, 2.0f, 2.0f, 2.0f, 2),
                        // the last key point is always the same as the enhanced location
                        // https://docs.mapbox.com/android/navigation/api/2.8.0/libnavigation-core/com.mapbox.navigation.core.trip.session/-location-matcher-result/key-points.html
                        createAndroidLocation(3.0, 3.0, 3.0, 3.0f, 3.0f, 3.0f, 3),
                    )
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 3.0)
                    Assert.assertTrue(it.longitude == 3.0)
                    Assert.assertTrue(it.altitude == 3.0)
                    Assert.assertTrue(it.accuracy == 3.0f)
                    Assert.assertTrue(it.bearing == 3.0f)
                    Assert.assertTrue(it.speed == 3.0f)
                    Assert.assertTrue(it.time > 3) // should now be current time
                },
                withArg {
                    Assert.assertTrue(it.size == 2)
                    Assert.assertTrue(it[0].latitude == 1.0)
                    Assert.assertTrue(it[0].longitude == 1.0)
                    Assert.assertTrue(it[0].altitude == 1.0)
                    Assert.assertTrue(it[0].accuracy == 1.0f)
                    Assert.assertTrue(it[0].bearing == 1.0f)
                    Assert.assertTrue(it[0].speed == 1.0f)
                    Assert.assertTrue(it[0].time > 1) // should now be current time
                    Assert.assertTrue(it[1].latitude == 2.0)
                    Assert.assertTrue(it[1].longitude == 2.0)
                    Assert.assertTrue(it[1].altitude == 2.0)
                    Assert.assertTrue(it[1].accuracy == 2.0f)
                    Assert.assertTrue(it[1].bearing == 2.0f)
                    Assert.assertTrue(it[1].speed == 2.0f)
                    Assert.assertTrue(it[1].time > 2) // should now be current time
                }
            )
        }
    }

    @Test
    fun `Should suppress invalid intermediate location updates and forward others with repaired accuracy, bearing and speed`() {
        val mapboxLocationObserver =
            mapboxLocationObserverProvider.createLocationObserver(mockLocationUpdatesObserver)
        every {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                any(),
                any()
            )
        } returns mockk()
        runBlocking {
            mapboxLocationObserver.onNewLocationMatcherResult(
                createMapboxLocationMatcherResult(
                    createAndroidLocation(6.0, 6.0, 6.0, 6.0f, 6.0f, 6.0f, 6),
                    listOf(
                        createAndroidLocation(1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 1),
                        createAndroidLocation(
                            Double.NaN,
                            2.0,
                            2.0,
                            2.0f,
                            2.0f,
                            2.0f,
                            2
                        ), // invalid, should be suppressed
                        createAndroidLocation(
                            3.0,
                            Double.NaN,
                            3.0,
                            3.0f,
                            3.0f,
                            3.0f,
                            3
                        ), // invalid, should be suppressed
                        createAndroidLocation(
                            4.0,
                            4.0,
                            Double.NaN,
                            4.0f,
                            4.0f,
                            4.0f,
                            4
                        ), // invalid, should be suppressed
                        createAndroidLocation(
                            5.0,
                            5.0,
                            5.0,
                            Float.NaN,
                            Float.NaN,
                            Float.NaN,
                            5
                        ), // should be repaired
                        // the last key point is always the same as the enhanced location
                        // https://docs.mapbox.com/android/navigation/api/2.8.0/libnavigation-core/com.mapbox.navigation.core.trip.session/-location-matcher-result/key-points.html
                        createAndroidLocation(6.0, 6.0, 6.0, 6.0f, 6.0f, 6.0f, 6),
                    )
                )
            )
        }
        verify {
            mockLocationUpdatesObserver.onEnhancedLocationChanged(
                withArg {
                    Assert.assertTrue(it.latitude == 6.0)
                    Assert.assertTrue(it.longitude == 6.0)
                    Assert.assertTrue(it.altitude == 6.0)
                    Assert.assertTrue(it.accuracy == 6.0f)
                    Assert.assertTrue(it.bearing == 6.0f)
                    Assert.assertTrue(it.speed == 6.0f)
                    Assert.assertTrue(it.time > 6) // should now be current time
                },
                withArg {
                    Assert.assertTrue(it.size == 2)
                    Assert.assertTrue(it[0].latitude == 1.0)
                    Assert.assertTrue(it[0].longitude == 1.0)
                    Assert.assertTrue(it[0].altitude == 1.0)
                    Assert.assertTrue(it[0].accuracy == 1.0f)
                    Assert.assertTrue(it[0].bearing == 1.0f)
                    Assert.assertTrue(it[0].speed == 1.0f)
                    Assert.assertTrue(it[0].time > 1) // should now be current time
                    Assert.assertTrue(it[1].latitude == 5.0)
                    Assert.assertTrue(it[1].longitude == 5.0)
                    Assert.assertTrue(it[1].altitude == 5.0)
                    Assert.assertTrue(it[1].accuracy == -1.0f)
                    Assert.assertTrue(it[1].bearing == -1.0f)
                    Assert.assertTrue(it[1].speed == -1.0f)
                    Assert.assertTrue(it[1].time > 5) // should now be current time
                }
            )
        }
    }

    private fun createAndroidLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        accuracy: Float?,
        bearing: Float?,
        speed: Float?,
        time: Long,
    ): android.location.Location {
        val location = mockk<android.location.Location>()
        every { location.latitude } returns latitude
        every { location.longitude } returns longitude
        every { location.altitude } returns altitude
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
}
