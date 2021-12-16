package com.ably.tracking.example.subscriber

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlin.math.roundToInt

fun getMarkerResourceIdByBearing(bearing: Float, isRaw: Boolean): Int {
    return when (bearing.roundToInt()) {
        in 23..67 -> if (isRaw) R.drawable.driver_raw_ne else R.drawable.driver_ne
        in 67..113 -> if (isRaw) R.drawable.driver_raw_e else R.drawable.driver_e
        in 113..158 -> if (isRaw) R.drawable.driver_raw_se else R.drawable.driver_se
        in 158..203 -> if (isRaw) R.drawable.driver_raw_s else R.drawable.driver_s
        in 203..247 -> if (isRaw) R.drawable.driver_raw_sw else R.drawable.driver_sw
        in 247..292 -> if (isRaw) R.drawable.driver_raw_w else R.drawable.driver_w
        in 292..337 -> if (isRaw) R.drawable.driver_raw_nw else R.drawable.driver_nw
        else -> if (isRaw) R.drawable.driver_raw_n else R.drawable.driver_n
    }
}

fun animateMarkerAndCircleMovement(marker: Marker, finalPosition: LatLng, circle: Circle, finalRadius: Double) {
    val startPosition = marker.position
    val startRadius = circle.radius
    val interpolator = AccelerateDecelerateInterpolator()
    val startTimeInMillis = SystemClock.uptimeMillis()
    val animationDurationInMillis = 1000f // this should probably match events update rate
    val nextCalculationDelayInMillis = 16L
    val handler = Handler(Looper.getMainLooper())

    handler.post(object : Runnable {
        var timeElapsedFromStartInMillis = 0L
        var timeProgressPercentage = 0f
        var distanceProgressPercentage = 0f
        override fun run() {
            timeElapsedFromStartInMillis = SystemClock.uptimeMillis() - startTimeInMillis
            timeProgressPercentage = timeElapsedFromStartInMillis / animationDurationInMillis
            distanceProgressPercentage = interpolator.getInterpolation(timeProgressPercentage)
            marker.position = interpolateLinear(distanceProgressPercentage, startPosition, finalPosition)
            circle.center = interpolateLinear(distanceProgressPercentage, startPosition, finalPosition)
            circle.radius = interpolateLinear(distanceProgressPercentage, startRadius, finalRadius)

            if (timeProgressPercentage < 1) {
                handler.postDelayed(this, nextCalculationDelayInMillis)
            }
        }
    })
}

private fun interpolateLinear(fraction: Float, a: LatLng, b: LatLng): LatLng {
    val lat = interpolateLinear(fraction, a.latitude, b.latitude)
    val lng = interpolateLinear(fraction, a.longitude, b.longitude)
    return LatLng(lat, lng)
}

private fun interpolateLinear(fraction: Float, a: Double, b: Double): Double = (b - a) * fraction + a
