package com.ably.tracking.publisher.debug

import android.app.PendingIntent
import android.os.Looper
import android.os.SystemClock
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.message.getLocationMessages
import com.ably.tracking.common.logging.d
import com.ably.tracking.common.logging.i
import com.ably.tracking.common.message.synopsis
import com.ably.tracking.common.message.toTracking
import com.ably.tracking.logging.LogHandler
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions

internal class AblySimulationLocationEngine(
    ablyOptions: ClientOptions,
    simulationTrackingId: String,
    logHandler: LogHandler?
) :
    LocationEngine {
    private val gson = Gson()
    private var lastLocationResult: LocationEngineResult? = null
    private val registeredListeners = mutableListOf<LocationEngineCallback<LocationEngineResult>>()
    private val lastLocationListeners =
        mutableListOf<LocationEngineCallback<LocationEngineResult>>()

    init {
        val ably = AblyRealtime(ablyOptions)
        val simulationChannel = ably.channels.get(simulationTrackingId)

        ably.connection.on { logHandler?.i("Ably connection state change: $it") }
        simulationChannel.on { logHandler?.i("Ably channel state change: $it") }

        simulationChannel.subscribe(EventNames.ENHANCED) { message ->
            logHandler?.i("Ably channel message: $message")
            message.getLocationMessages(gson).forEach {
                logHandler?.d("Received enhanced location: ${it.synopsis()}")
                val loc = it.toTracking().toAndroid()
                loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                onLocationEngineResult(LocationEngineResult.create(loc))
            }
        }
    }

    @Synchronized
    private fun onLocationEngineResult(locationEngineResult: LocationEngineResult) {
        lastLocationResult = locationEngineResult
        if (lastLocationListeners.isNotEmpty()) {
            lastLocationListeners.forEach { it.onSuccess(locationEngineResult) }
            lastLocationListeners.clear()
        }
        registeredListeners.forEach { it.onSuccess(locationEngineResult) }
    }

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        if (lastLocationResult != null) {
            callback.onSuccess(lastLocationResult)
        } else {
            lastLocationListeners.add(callback)
        }
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        registeredListeners.add(callback)
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        pendingIntent: PendingIntent?
    ) {
        throw UnsupportedOperationException("requestLocationUpdates with intents is unsupported")
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        registeredListeners.remove(callback)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException("removeLocationUpdates with intents is unsupported")
    }
}
