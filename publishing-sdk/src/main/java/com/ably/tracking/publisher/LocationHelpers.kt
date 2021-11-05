package com.ably.tracking.publisher

import com.ably.tracking.Location
import com.ably.tracking.locationprovider.Destination
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal fun Location.distanceInMetersFrom(location: Location): Double =
    distance(latitude, longitude, location.latitude, location.longitude)

internal fun Location.distanceInMetersFrom(destination: Destination): Double =
    distance(latitude, longitude, destination.latitude, destination.longitude)

private fun distance(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
    val MAXITERS = 20
// Convert lat/long to radians
    val lat1 = latitude1 * Math.PI / 180.0
    val lat2 = latitude2 * Math.PI / 180.0
    val lon1 = longitude1 * Math.PI / 180.0
    val lon2 = longitude2 * Math.PI / 180.0

    val a = 6378137.0 // WGS84 major axis
    val b = 6356752.3142 // WGS84 semi-major axis
    val f = (a - b) / a
    val aSqMinusBSqOverBSq = (a * a - b * b) / (b * b)

    val L = lon2 - lon1
    var A = 0.0
    val U1 = atan((1.0 - f) * tan(lat1))
    val U2 = atan((1.0 - f) * tan(lat2))

    val cosU1 = cos(U1)
    val cosU2 = cos(U2)
    val sinU1 = sin(U1)
    val sinU2 = sin(U2)
    val cosU1cosU2 = cosU1 * cosU2
    val sinU1sinU2 = sinU1 * sinU2

    var sigma = 0.0
    var deltaSigma = 0.0
    var cosSqAlpha: Double
    var cos2SM: Double
    var cosSigma: Double
    var sinSigma: Double
    var cosLambda: Double
    var sinLambda: Double

    var lambda = L // initial guess
    for (iter in 0..MAXITERS) {
        val lambdaOrig = lambda
        cosLambda = cos(lambda)
        sinLambda = sin(lambda)
        val t1 = cosU2 * sinLambda
        val t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
        val sinSqSigma = t1 * t1 + t2 * t2 // (14)
        sinSigma = sqrt(sinSqSigma)
        cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda // (15)
        sigma = atan2(sinSigma, cosSigma) // (16)
        val sinAlpha = if (sinSigma == 0.0) 0.0 else cosU1cosU2 * sinLambda / sinSigma // (17)
        cosSqAlpha = 1.0 - sinAlpha * sinAlpha
        cos2SM = if (cosSqAlpha == 0.0) 0.0 else cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha // (18)

        val uSquared = cosSqAlpha * aSqMinusBSqOverBSq // defn
        A = 1 + (uSquared / 16384.0) * (4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)))
        val B = (uSquared / 1024.0) * (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)))
        val C = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha)) // (10)
        val cos2SMSq = cos2SM * cos2SM
        deltaSigma = B * sinSigma * (cos2SM + (B / 4.0) * (cosSigma * (-1.0 + 2.0 * cos2SMSq) - (B / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)))

        lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM))) // (11)

        val delta = (lambda - lambdaOrig) / lambda
        if (abs(delta) < 1.0e-12) {
            break
        }
    }

    val distance: Double = (b * A * (sigma - deltaSigma))
    return distance
}

private fun Destination.toAndroidLocation() =
    android.location.Location("fused").apply {
        longitude = this@toAndroidLocation.longitude
        latitude = this@toAndroidLocation.latitude
    }

internal fun Location.timeFrom(location: Location): Long = abs(time - location.time)
