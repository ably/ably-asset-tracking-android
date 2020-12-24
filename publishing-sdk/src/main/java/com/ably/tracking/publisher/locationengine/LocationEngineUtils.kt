package com.ably.tracking.publisher.locationengine

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal object LocationEngineUtils {
    private const val GOOGLE_LOCATION_SERVICES = "com.google.android.gms.location.LocationServices"
    private const val GOOGLE_API_AVAILABILITY = "com.google.android.gms.common.GoogleApiAvailability"

    fun hasGoogleLocationServices(context: Context): Boolean {
        var hasGoogleLocationServices = isOnClasspath(GOOGLE_LOCATION_SERVICES)
        if (isOnClasspath(GOOGLE_API_AVAILABILITY)) {
            // Check Google Play services APK is available and up-to-date on this device
            hasGoogleLocationServices = hasGoogleLocationServices &&
                (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
        }
        return hasGoogleLocationServices
    }

    private fun isOnClasspath(className: String): Boolean =
        try {
            Class.forName(className)
            true
        } catch (exception: ClassNotFoundException) {
            false
        }
}
