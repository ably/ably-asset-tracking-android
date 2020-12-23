package com.ably.tracking.publisher.locationengine

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Looper
import com.ably.tracking.Resolution
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult

@SuppressLint("MissingPermission")
class GoogleLocationEngine(context: Context) : ResolutionLocationEngine {
    private val listeners: MutableMap<LocationEngineCallback<LocationEngineResult>, LocationCallback> = mutableMapOf()
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun changeResolution(resolution: Resolution) {
        val request = resolution.toLocationEngineRequest()
        listeners.values.forEach { fusedLocationProviderClient.removeLocationUpdates(it) }
        listeners.keys.forEach { requestLocationUpdates(request, it, Looper.getMainLooper()) }
    }

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        val wrapper = LastLocationListenersWrapper(callback)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(wrapper).addOnFailureListener(wrapper)
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        fusedLocationProviderClient.requestLocationUpdates(
            toGMSLocationRequest(request),
            getListenerForCallback(callback),
            looper
        )
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        fusedLocationProviderClient.requestLocationUpdates(toGMSLocationRequest(request), pendingIntent)
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        listeners.remove(callback)?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        pendingIntent?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }

    private fun getListenerForCallback(callback: LocationEngineCallback<LocationEngineResult>): LocationCallback =
        listeners[callback] ?: LocationCallbackWrapper(callback).apply { listeners[callback] = this }

    private fun toGMSLocationRequest(request: LocationEngineRequest): LocationRequest =
        LocationRequest().apply {
            interval = request.interval
            fastestInterval = request.fastestInterval
            smallestDisplacement = request.displacement
            maxWaitTime = request.maxWaitTime
            priority = toGMSLocationPriority(request.priority)
        }

    private fun toGMSLocationPriority(enginePriority: Int): Int =
        when (enginePriority) {
            LocationEngineRequest.PRIORITY_HIGH_ACCURACY -> LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            LocationEngineRequest.PRIORITY_LOW_POWER -> LocationRequest.PRIORITY_LOW_POWER
            else -> LocationRequest.PRIORITY_NO_POWER
        }

    private class LocationCallbackWrapper(private val callback: LocationEngineCallback<LocationEngineResult>) :
        LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.locations.let { locations ->
                if (locations.isNotEmpty()) {
                    callback.onSuccess(LocationEngineResult.create(locations))
                } else {
                    callback.onFailure(Exception("Unavailable location"))
                }
            }
        }
    }

    internal class LastLocationListenersWrapper(private val callback: LocationEngineCallback<LocationEngineResult>) :
        OnSuccessListener<Location?>, OnFailureListener {
        override fun onSuccess(location: Location?) {
            callback.onSuccess(LocationEngineResult.create(location))
        }

        override fun onFailure(e: Exception) {
            callback.onFailure(e)
        }
    }
}
