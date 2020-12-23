package com.ably.tracking.publisher.locationengine

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import timber.log.Timber

@SuppressLint("MissingPermission")
open class FusedAndroidLocationEngine(context: Context) : ResolutionLocationEngine {
    private val listeners: MutableMap<LocationEngineCallback<LocationEngineResult>, LocationListener> = mutableMapOf()
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val DEFAULT_PROVIDER = LocationManager.PASSIVE_PROVIDER
    private var currentProvider = DEFAULT_PROVIDER

    override fun changeResolution(resolution: Resolution) {
        TODO("Not yet implemented")
    }

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        getBestLastLocation().let {
            if (it != null) {
                callback.onSuccess(LocationEngineResult.create(it))
            } else {
                callback.onFailure(Exception("Last location unavailable"))
            }
        }
    }

    private fun getBestLastLocation(): Location? {
        var bestLastLocation: Location? = null
        locationManager.allProviders.forEach { provider ->
            getLastLocationFor(provider)?.let { location ->
                if (isNewLocationBetter(location, bestLastLocation)) {
                    bestLastLocation = location
                }
            }
        }
        return bestLastLocation
    }

    private fun getLastLocationFor(provider: String): Location? =
        try {
            locationManager.getLastKnownLocation(provider)
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
            null
        }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        currentProvider = getBestProvider(request.priority)
        val listener = getListenerForCallback(callback)
        locationManager.requestLocationUpdates(
            currentProvider, request.interval, request.displacement, listener, looper
        )

        if (shouldStartNetworkProvider(request.priority)) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, request.interval, request.displacement, listener, looper
                )
            } catch (exception: IllegalArgumentException) {
                Timber.e(exception)
            }
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        pendingIntent?.let {
            currentProvider = getBestProvider(request.priority)
            locationManager.requestLocationUpdates(currentProvider, request.interval, request.displacement, it)

            if (shouldStartNetworkProvider(request.priority)) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, request.interval, request.displacement, pendingIntent
                    )
                } catch (exception: IllegalArgumentException) {
                    Timber.e(exception)
                }
            }
        }
    }

    private fun shouldStartNetworkProvider(priority: Int): Boolean =
        (
            (
                priority == LocationEngineRequest.PRIORITY_HIGH_ACCURACY ||
                    priority == LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY
                ) &&
                currentProvider == LocationManager.GPS_PROVIDER
            )

    private fun getBestProvider(priority: Int): String =
        if (priority == LocationEngineRequest.PRIORITY_NO_POWER) {
            DEFAULT_PROVIDER
        } else {
            locationManager.getBestProvider(getCriteria(priority), true) ?: DEFAULT_PROVIDER
        }

    private fun getCriteria(priority: Int): Criteria =
        Criteria().apply {
            accuracy = priorityToAccuracy(priority)
            isCostAllowed = true
            powerRequirement = priorityToPowerRequirement(priority)
        }

    private fun priorityToAccuracy(priority: Int): Int =
        when (priority) {
            LocationEngineRequest.PRIORITY_HIGH_ACCURACY, LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Criteria.ACCURACY_FINE
            else -> Criteria.ACCURACY_COARSE
        }

    private fun priorityToPowerRequirement(priority: Int): Int =
        when (priority) {
            LocationEngineRequest.PRIORITY_HIGH_ACCURACY -> Criteria.POWER_HIGH
            LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Criteria.POWER_MEDIUM
            else -> Criteria.POWER_LOW
        }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        listeners.remove(callback)?.let { locationManager.removeUpdates(it) }
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        pendingIntent?.let { locationManager.removeUpdates(it) }
    }

    private fun getListenerForCallback(callback: LocationEngineCallback<LocationEngineResult>): LocationListener =
        listeners[callback] ?: LocationListenerWrapper(callback).apply { listeners[callback] = this }

    private inner class LocationListenerWrapper(private val callback: LocationEngineCallback<LocationEngineResult>?) :
        LocationListener {
        private var currentBestLocation: Location? = null

        override fun onLocationChanged(location: Location) {
            if (isNewLocationBetter(location, currentBestLocation)) {
                currentBestLocation = location
            }
            callback?.onSuccess(LocationEngineResult.create(currentBestLocation))
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Timber.d("onStatusChanged: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Timber.d("onProviderEnabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Timber.d("onProviderDisabled: $provider")
        }
    }

    private fun isNewLocationBetter(newLocation: Location, currentBestLocation: Location?): Boolean {
        val timeThresholdInMilliseconds = MILLISECONDS_PER_SECOND * 2
        val accuracyThresholdInMeters = 200

        if (currentBestLocation == null) {
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDifference = newLocation.time - currentBestLocation.time
        val isNewLocationNewer = timeDifference > 0
        val isNewLocationSignificantlyNewer = timeDifference > timeThresholdInMilliseconds
        val isNewLocationSignificantlyOlder = timeDifference < -timeThresholdInMilliseconds

        // Check whether the new location fix is more or less accurate
        val accuracyDifference = (newLocation.accuracy - currentBestLocation.accuracy).toInt()
        val isNewLocationLessAccurate = accuracyDifference > 0
        val isNewLocationMoreAccurate = accuracyDifference < 0
        val isNewLocationSignificantlyLessAccurate = accuracyDifference > accuracyThresholdInMeters

        val isFromSameProvider = newLocation.provider == currentBestLocation.provider

        return when {
            isNewLocationSignificantlyNewer -> true
            isNewLocationSignificantlyOlder -> false
            isNewLocationMoreAccurate -> true
            isNewLocationNewer && !isNewLocationLessAccurate -> true
            isNewLocationNewer && !isNewLocationSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }
}
