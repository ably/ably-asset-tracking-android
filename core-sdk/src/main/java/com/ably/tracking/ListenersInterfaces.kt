package com.ably.tracking

import android.location.Location

interface LocationUpdatedListener {
    fun onLocationUpdated(location: Location)
}
