package com.ably.tracking.publisher

import com.ably.tracking.locationprovider.Destination
import com.ably.tracking.test.common.anyLocation
import com.ably.tracking.test.common.createLocation
import org.junit.Assert
import org.junit.Test

internal class ThresholdCheckerTest {
    val thresholdChecker = ThresholdChecker()

    @Test
    fun `threshold is not reached when current location is not within spatial threshold from destination`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0)
        val currentLocation = createLocation(10.0, 10.0)
        val destination = Destination(1.0, 1.0)

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            System.currentTimeMillis(),
            destination,
            null
        )

        // then
        Assert.assertFalse(isThresholdReached)
    }

    @Test
    fun `threshold is reached when current location is within spatial threshold from destination`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0)
        val currentLocation = createLocation(1.000001, 1.0)
        val destination = Destination(1.0, 1.0)

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            System.currentTimeMillis(),
            destination,
            null
        )

        // then
        Assert.assertTrue(isThresholdReached)
    }

    @Test
    fun `threshold is not reached when current time is not within temporal threshold from estimated arrival time`() {
        // given
        val threshold = DefaultProximity(temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val estimatedArrivalTimeInMilliseconds = currentTime + 1000L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            anyLocation(),
            currentTime,
            null,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertFalse(isThresholdReached)
    }

    @Test
    fun `threshold is reached when current time is within temporal threshold from estimated arrival time`() {
        // given
        val threshold = DefaultProximity(temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val estimatedArrivalTimeInMilliseconds = currentTime + 300L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            anyLocation(),
            currentTime,
            null,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertTrue(isThresholdReached)
    }

    @Test
    fun `threshold is reached when spatial threshold is reached but temporal threshold isn't reached`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0, temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val currentLocation = createLocation(1.000001, 1.0)
        val destination = Destination(1.0, 1.0)
        val estimatedArrivalTimeInMilliseconds = currentTime + 1500L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            currentTime,
            destination,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertTrue(isThresholdReached)
    }

    @Test
    fun `threshold is reached when spatial threshold isn't reached but temporal threshold is reached`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0, temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val currentLocation = createLocation(20.0, 20.0)
        val destination = Destination(1.0, 1.0)
        val estimatedArrivalTimeInMilliseconds = currentTime + 300L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            currentTime,
            destination,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertTrue(isThresholdReached)
    }

    @Test
    fun `threshold is reached when both spatial threshold and temporal thresholds are reached`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0, temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val currentLocation = createLocation(1.1, 1.1)
        val destination = Destination(1.0, 1.0)
        val estimatedArrivalTimeInMilliseconds = currentTime + 300L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            currentTime,
            destination,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertTrue(isThresholdReached)
    }

    @Test
    fun `threshold is not reached when both spatial threshold and temporal thresholds aren't reached`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0, temporal = 500L)
        val currentTime = System.currentTimeMillis()
        val currentLocation = createLocation(20.0, 20.0)
        val destination = Destination(1.0, 1.0)
        val estimatedArrivalTimeInMilliseconds = currentTime + 1500L

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            currentLocation,
            currentTime,
            destination,
            estimatedArrivalTimeInMilliseconds
        )

        // then
        Assert.assertFalse(isThresholdReached)
    }

    @Test
    fun `threshold is not reached when both destination and estimated arrival time are nulls`() {
        // given
        val threshold = DefaultProximity(spatial = 1.0, temporal = 500L)

        // when
        val isThresholdReached = thresholdChecker.isThresholdReached(
            threshold,
            anyLocation(),
            System.currentTimeMillis(),
            null,
            null
        )

        // then
        Assert.assertFalse(isThresholdReached)
    }
}
