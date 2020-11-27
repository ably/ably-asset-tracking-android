package com.ably.tracking.example.subscriber

import kotlin.math.roundToInt

fun getMarkerResourceIdByBearing(bearing: Float): Int {
    return when (bearing.roundToInt()) {
        in 0..23, in 337..361 -> R.drawable.driver_n
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
