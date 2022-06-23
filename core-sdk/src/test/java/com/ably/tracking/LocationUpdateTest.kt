package com.ably.tracking

import com.ably.tracking.test.common.anyLocation
import com.ably.tracking.test.common.createLocation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LocationUpdateTest(locationUpdateType: String) {
    private val isTestingEnhancedLocationUpdate = locationUpdateType == ENHANCED

    companion object {
        private const val ENHANCED = "ENHANCED"
        private const val RAW = "RAW"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(ENHANCED), arrayOf(RAW))
    }

    @Test
    fun `location updates with the same locations should be equal`() {
        // given
        val firstLocationUpdate = createLocationUpdate(location = firstLocation)
        val secondLocationUpdate = createLocationUpdate(location = firstLocation)

        // when
        val result = firstLocationUpdate == secondLocationUpdate

        // then
        Assert.assertTrue(result)
    }

    @Test
    fun `location updates with different locations should not be equal`() {
        // given
        val firstLocationUpdate = createLocationUpdate(location = firstLocation)
        val secondLocationUpdate = createLocationUpdate(location = secondLocation)

        // when
        val result = firstLocationUpdate == secondLocationUpdate

        // then
        Assert.assertFalse(result)
    }

    @Test
    fun `location updates with the same skipped locations should be equal`() {
        // given
        val firstLocationUpdate = createLocationUpdate(skippedLocations = listOf(firstLocation))
        val secondLocationUpdate = createLocationUpdate(skippedLocations = listOf(firstLocation))

        // when
        val result = firstLocationUpdate == secondLocationUpdate

        // then
        Assert.assertTrue(result)
    }

    @Test
    fun `location updates with different skipped locations should not be equal`() {
        // given
        val firstLocationUpdate = createLocationUpdate(skippedLocations = listOf(firstLocation))
        val secondLocationUpdate = createLocationUpdate(skippedLocations = listOf(secondLocation))

        // when
        val result = firstLocationUpdate == secondLocationUpdate

        // then
        Assert.assertFalse(result)
    }

    private fun createLocationUpdate(
        location: Location = anyLocation(),
        skippedLocations: List<Location> = emptyList()
    ): LocationUpdate =
        if (isTestingEnhancedLocationUpdate)
            EnhancedLocationUpdate(location, skippedLocations, listOf(), LocationUpdateType.ACTUAL)
        else
            LocationUpdate(location, skippedLocations)

    private val firstLocation = createLocation(1.0, 1.0)
    private val secondLocation = createLocation(2.0, 2.0)
}
