package com.ably.tracking.publisher.debug

import android.app.PendingIntent
import android.os.Looper
import com.ably.tracking.logging.LogHandler
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult

internal open class BaseLocationEngine(protected val logHandler: LogHandler?) : LocationEngine {
    private var lastLocationResult: LocationEngineResult? = null
    private val registeredListeners = mutableListOf<LocationEngineCallback<LocationEngineResult>>()
    private val lastLocationListeners =
        mutableListOf<LocationEngineCallback<LocationEngineResult>>()

    @Synchronized
    protected fun onLocationEngineResult(locationEngineResult: LocationEngineResult) {
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
