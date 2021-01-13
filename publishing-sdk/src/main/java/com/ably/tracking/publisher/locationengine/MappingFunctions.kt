package com.ably.tracking.publisher.locationengine

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.mapbox.android.core.location.LocationEngineRequest

internal fun Resolution.toLocationEngineRequest(): LocationEngineRequest =
    LocationEngineRequest.Builder(desiredInterval)
        .setDisplacement(minimumDisplacement.toFloat())
        .setPriority(getPriority())
        .build()

private fun Resolution.getPriority(): Int =
    when (accuracy) {
        Accuracy.MINIMUM -> LocationEngineRequest.PRIORITY_NO_POWER
        Accuracy.LOW -> LocationEngineRequest.PRIORITY_LOW_POWER
        Accuracy.BALANCED -> LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY
        Accuracy.HIGH -> LocationEngineRequest.PRIORITY_HIGH_ACCURACY
        Accuracy.MAXIMUM -> LocationEngineRequest.PRIORITY_HIGH_ACCURACY
    }
