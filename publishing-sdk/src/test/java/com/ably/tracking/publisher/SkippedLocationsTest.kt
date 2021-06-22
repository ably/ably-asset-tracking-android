package com.ably.tracking.publisher

import com.ably.tracking.test.common.createLocation
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SkippedLocationsTest {
    private val trackableId = "test-trackable-id"
    private lateinit var skippedLocations: SkippedLocations

    @Before
    fun beforeEach() {
        skippedLocations = SkippedLocations()
    }

    @Test
    fun `Should return empty list if no location was added`() {
        // given

        // when
        val skippedLocationsList = skippedLocations.toList(trackableId)

        // then
        Assert.assertTrue(skippedLocationsList.isEmpty())
    }

    @Test
    fun `Should return locations sorted by time`() {
        // given
        val locations = listOf(
            createLocation(timestamp = 4),
            createLocation(timestamp = 1),
            createLocation(timestamp = 3),
            createLocation(timestamp = 2)
        )

        // when
        locations.forEach {
            skippedLocations.add(trackableId, it)
        }
        val skippedLocationsList = skippedLocations.toList(trackableId)

        // then
        val skippedLocationsTimestamps = skippedLocationsList.map { it.time }
        Assert.assertEquals(listOf<Long>(1, 2, 3, 4), skippedLocationsTimestamps)
    }

    @Test
    fun `Should keep the list within the maximum size`() {
        // given
        val locations = (0..100).map { createLocation(timestamp = it.toLong()) }

        // when
        locations.forEach {
            skippedLocations.add(trackableId, it)
        }
        val skippedLocationsList = skippedLocations.toList(trackableId)

        // then
        Assert.assertEquals(60, skippedLocationsList.size)
    }

    @Test
    fun `Should remove oldest locations when adding new ones if list limit is exceeded`() {
        // given
        val locations = (100 downTo 0).map { createLocation(timestamp = it.toLong()) }

        // when
        locations.forEach {
            skippedLocations.add(trackableId, it)
        }
        val skippedLocationsList = skippedLocations.toList(trackableId)

        // then
        val skippedLocationsTimestamps = skippedLocationsList.map { it.time }
        Assert.assertEquals(41, skippedLocationsTimestamps.first())
        Assert.assertEquals(100, skippedLocationsTimestamps.last())
    }

    @Test
    fun `Should return an empty list if skipped locations were cleared for the trackable ID`() {
        // given
        val locations = (0..10).map { createLocation(timestamp = it.toLong()) }

        // when
        locations.forEach { skippedLocations.add(trackableId, it) }
        skippedLocations.clear(trackableId)
        val skippedLocationsList = skippedLocations.toList(trackableId)

        // then
        Assert.assertTrue(skippedLocationsList.isEmpty())
    }

    @Test
    fun `Should return empty lists if skipped locations were cleared for all trackables`() {
        // given
        val anotherTrackableId = "some-other-test-trackable-id"
        val locations = (0..10).map { createLocation(timestamp = it.toLong()) }

        // when
        locations.forEach {
            skippedLocations.add(trackableId, it)
            skippedLocations.add(anotherTrackableId, it)
        }
        skippedLocations.clearAll()
        val skippedLocationsList = skippedLocations.toList(trackableId)
        val anotherSkippedLocationsList = skippedLocations.toList(anotherTrackableId)

        // then
        Assert.assertTrue(skippedLocationsList.isEmpty())
        Assert.assertTrue(anotherSkippedLocationsList.isEmpty())
    }

    @Test
    fun `Should clear only skipped location for the specified trackable ID`() {
        // given
        val anotherTrackableId = "some-other-test-trackable-id"
        val locations = (0..10).map { createLocation(timestamp = it.toLong()) }

        // when
        locations.forEach {
            skippedLocations.add(trackableId, it)
            skippedLocations.add(anotherTrackableId, it)
        }
        skippedLocations.clear(trackableId)
        val skippedLocationsList = skippedLocations.toList(trackableId)
        val anotherSkippedLocationsList = skippedLocations.toList(anotherTrackableId)

        // then
        Assert.assertTrue(skippedLocationsList.isEmpty())
        Assert.assertTrue(anotherSkippedLocationsList.isNotEmpty())
    }
}
