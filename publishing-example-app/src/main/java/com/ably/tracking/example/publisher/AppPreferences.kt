package com.ably.tracking.example.publisher

import android.content.Context
import androidx.preference.PreferenceManager

class AppPreferences(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val LOCATION_SOURCE_KEY = context.getString(R.string.preferences_location_source_key)
    private val SIMULATION_CHANNEL_KEY =
        context.getString(R.string.preferences_simulation_channel_name_key)
    private val S3_FILE_KEY = context.getString(R.string.preferences_s3_file_key)
    private val DEFAULT_LOCATION_SOURCE = context.getString(R.string.default_location_source)
    private val DEFAULT_SIMULATION_CHANNEL = context.getString(R.string.default_simulation_channel)
    private val DEFAULT_S3_FILE = ""

    fun getLocationSource(): String =
        preferences.getString(LOCATION_SOURCE_KEY, DEFAULT_LOCATION_SOURCE)!!

    fun getSimulationChannel() =
        preferences.getString(SIMULATION_CHANNEL_KEY, DEFAULT_SIMULATION_CHANNEL)!!

    fun getS3File() =
        preferences.getString(S3_FILE_KEY, DEFAULT_S3_FILE)!!
}
