package com.ably.tracking.publisher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.math.MathUtils.clamp

internal interface BatteryDataProvider {

    /**
     * Returns current battery value in the range 0.0f (no battery) to 100.0f (full battery).
     * If the battery status isn't available it returns null.
     *
     * @return Current battery value in the range 0.0f to 100.0f or null if data not available.
     */
    fun getCurrentBatteryPercentage(): Float?
}

/**
 * Based on https://developer.android.com/training/monitoring-device-state/battery-monitoring
 */
internal class DefaultBatteryDataProvider(private val context: Context) : BatteryDataProvider {
    private val MINIMUM_BATTERY_PERCENTAGE = 0.0f
    private val MAXIMUM_BATTERY_PERCENTAGE = 100.0f

    override fun getCurrentBatteryPercentage(): Float? =
        getCurrentBatteryPercentage(context)

    private fun getCurrentBatteryPercentage(context: Context): Float? =
        getCurrentBatteryStatusIntent(context)?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            calculateBatteryPercentage(level, scale)
        }

    fun calculateBatteryPercentage(level: Int, scale: Int): Float {
        val batteryPercentage = level.toFloat() * 100.0f / scale.toFloat()
        return clamp(batteryPercentage, MINIMUM_BATTERY_PERCENTAGE, MAXIMUM_BATTERY_PERCENTAGE)
    }

    private fun getCurrentBatteryStatusIntent(context: Context): Intent? =
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { context.registerReceiver(null, it) }
}
