package com.ably.tracking

import android.location.Location
import io.ably.lib.realtime.ConnectionStateListener

interface LocationUpdatedListener {
    fun onLocationUpdated(location: Location)
}

interface AblyStateChangeListener {
    fun onConnectionStateChange(connectionStateChange: ConnectionStateListener.ConnectionStateChange)
}
