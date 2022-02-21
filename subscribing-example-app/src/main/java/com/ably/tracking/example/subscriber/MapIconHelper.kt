package com.ably.tracking.example.subscriber

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
