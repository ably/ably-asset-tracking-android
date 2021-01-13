package com.ably.tracking.publisher.locationengine

import com.ably.tracking.Resolution
import com.mapbox.android.core.location.LocationEngine

interface ResolutionLocationEngine : LocationEngine {
    fun changeResolution(resolution: Resolution)
}
