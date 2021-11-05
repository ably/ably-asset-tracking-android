package com.ably.tracking.locationprovider.mapbox.locationengine

import com.ably.tracking.Resolution
import com.mapbox.android.core.location.LocationEngine

interface ResolutionLocationEngine : LocationEngine {
    fun changeResolution(resolution: Resolution)
}
