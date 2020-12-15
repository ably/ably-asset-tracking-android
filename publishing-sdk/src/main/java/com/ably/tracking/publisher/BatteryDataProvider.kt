package com.ably.tracking.publisher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

internal interface BatteryDataProvider {

    /**
     * Returns current battery value in the range 0.0f (no battery) to 100.0f (full battery).
     *
     * @return Current battery value in the range 0.0f to 100.0f
     */
    fun getCurrentBatteryPercentage(): Float?
}

/**
 * Based on https://developer.android.com/training/monitoring-device-state/battery-monitoring
 */
internal class DefaultBatteryDataProvider(private val context: Context) : BatteryDataProvider {

    override fun getCurrentBatteryPercentage(): Float? =
        getCurrentBatteryPercentage(context)

    private fun getCurrentBatteryPercentage(context: Context): Float? =
        getCurrentBatteryStatusIntent(context)?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

    private fun getCurrentBatteryStatusIntent(context: Context): Intent? =
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { context.registerReceiver(null, it) }
}
