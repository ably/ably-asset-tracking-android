package com.ably.tracking.example.publisher

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet

// EXAMPLE SNIPPET FROM HERE:

val exampleConstraints = DefaultResolutionConstraints(
    DefaultResolutionSet(
        Resolution(
            accuracy = Accuracy.BALANCED,
            desiredInterval = 1000L, // milliseconds
            minimumDisplacement = 1.0 // metres
        )
    ),
    proximityThreshold = DefaultProximity(spatial = 1.0), // metres
    batteryLevelThreshold = 10.0f, // percent
    lowBatteryMultiplier = 2.0f
)
