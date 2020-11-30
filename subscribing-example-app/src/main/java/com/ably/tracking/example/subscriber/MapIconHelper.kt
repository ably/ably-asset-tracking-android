package com.ably.tracking.example.subscriber

import android.os.Handler
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlin.math.roundToInt

fun getMarkerResourceIdByBearing(bearing: Float): Int {
    return when (bearing.roundToInt()) {
        in 23..67 -> R.drawable.driver_ne
        in 67..113 -> R.drawable.driver_e
        in 113..158 -> R.drawable.driver_se
        in 158..203 -> R.drawable.driver_s
        in 203..247 -> R.drawable.driver_sw
        in 247..292 -> R.drawable.driver_w
        in 292..337 -> R.drawable.driver_nw
        else -> R.drawable.driver_n
    }
}

fun animateMarkerMovement(marker: Marker, finalPosition: LatLng) {
    val startPosition = marker.position
    val interpolator = AccelerateDecelerateInterpolator()
    val startTimeInMillis = SystemClock.uptimeMillis()
    val animationDurationInMillis = 1000f // this should probably match events update rate
    val nextCalculationDelayInMillis = 16L
    val handler = Handler()

    handler.post(object : Runnable {
        var timeElapsedFromStartInMillis = 0L
        var timeProgressPercentage = 0f
        var distanceProgressPercentage = 0f
        override fun run() {
            timeElapsedFromStartInMillis = SystemClock.uptimeMillis() - startTimeInMillis
            timeProgressPercentage = timeElapsedFromStartInMillis / animationDurationInMillis
            distanceProgressPercentage = interpolator.getInterpolation(timeProgressPercentage)
            marker.position =
                interpolateLinear(distanceProgressPercentage, startPosition, finalPosition)

            if (timeProgressPercentage < 1) {
                handler.postDelayed(this, nextCalculationDelayInMillis)
            }
        }
    })
}

private fun interpolateLinear(fraction: Float, a: LatLng, b: LatLng): LatLng {
    val lat = (b.latitude - a.latitude) * fraction + a.latitude
    val lng = (b.longitude - a.longitude) * fraction + a.longitude
    return LatLng(lat, lng)
}
