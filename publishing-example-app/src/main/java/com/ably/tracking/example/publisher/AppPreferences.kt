package com.ably.tracking.example.publisher

import android.content.Context
import androidx.preference.PreferenceManager
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution

class AppPreferences private constructor(context: Context) {
    companion object {
        private var instance: AppPreferences? = null
        fun getInstance(context: Context): AppPreferences {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AppPreferences(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val LOCATION_SOURCE_KEY = context.getString(R.string.preferences_location_source_key)
    private val SIMULATION_CHANNEL_KEY =
        context.getString(R.string.preferences_simulation_channel_name_key)
    private val S3_FILE_KEY = context.getString(R.string.preferences_s3_file_key)
    private val ROUTING_PROFILE_KEY = context.getString(R.string.preferences_routing_profile_key)
    private val VEHICLE_PROFILE_KEY = context.getString(R.string.preferences_vehicle_profile_key)
    private val RESOLUTION_ACCURACY_KEY = context.getString(R.string.preferences_resolution_accuracy_key)
    private val RESOLUTION_DESIRED_INTERVAL_KEY =
        context.getString(R.string.preferences_resolution_desired_interval_key)
    private val RESOLUTION_MINIMUM_DISPLACEMENT_KEY =
        context.getString(R.string.preferences_resolution_minimum_displacement_key)
    private val SEND_RAW_LOCATIONS_KEY = context.getString(R.string.preferences_send_raw_locations_key)
    private val SEND_RESOLUTION_KEY = context.getString(R.string.preferences_send_resolution_key)
    private val ENABLE_CONSTANT_LOCATION_ENGINE_RESOLUTION_KEY =
        context.getString(R.string.preferences_enable_constant_resolution_key)
    private val CONSTANT_RESOLUTION_ACCURACY_KEY =
        context.getString(R.string.preferences_constant_resolution_accuracy_key)
    private val CONSTANT_RESOLUTION_DESIRED_INTERVAL_KEY =
        context.getString(R.string.preferences_constant_resolution_desired_interval_key)
    private val CONSTANT_RESOLUTION_MINIMUM_DISPLACEMENT_KEY =
        context.getString(R.string.preferences_constant_resolution_minimum_displacement_key)
    val DEFAULT_LOCATION_SOURCE = LocationSourceType.PHONE.name
    private val DEFAULT_SIMULATION_CHANNEL = context.getString(R.string.default_simulation_channel)
    private val DEFAULT_S3_FILE = ""
    private val DEFAULT_ROUTING_PROFILE = RoutingProfileType.DRIVING.name
    private val DEFAULT_VEHICLE_PROFILE = VehicleProfileType.CAR.name
    private val DEFAULT_RESULTION_ACCURACY = Accuracy.BALANCED.name
    private val DEFAULT_RESULTION_DESIRED_INTERVAL = 1000L
    private val DEFAULT_RESULTION_MINIMUM_DISPLACEMENT = 1.0f
    private val DEFAULT_SEND_RAW_LOCATIONS = false
    private val DEFAULT_SEND_RESOLUTION = true
    private val DEFAULT_ENABLE_CONSTANT_LOCATION_ENGINE_RESOLUTION = false

    fun getLocationSource(): LocationSourceType =
        LocationSourceType.valueOf(preferences.getString(LOCATION_SOURCE_KEY, DEFAULT_LOCATION_SOURCE)!!)

    fun getSimulationChannel() =
        preferences.getString(SIMULATION_CHANNEL_KEY, DEFAULT_SIMULATION_CHANNEL)!!

    fun getS3File() =
        preferences.getString(S3_FILE_KEY, DEFAULT_S3_FILE)!!

    fun getRoutingProfile(): RoutingProfileType =
        RoutingProfileType.valueOf(preferences.getString(ROUTING_PROFILE_KEY, DEFAULT_ROUTING_PROFILE)!!)

    fun getVehicleProfile(): VehicleProfileType =
        VehicleProfileType.valueOf(preferences.getString(VEHICLE_PROFILE_KEY, DEFAULT_VEHICLE_PROFILE)!!)

    fun getResolutionAccuracy(): Accuracy =
        preferences.getString(RESOLUTION_ACCURACY_KEY, DEFAULT_RESULTION_ACCURACY)!!.let { Accuracy.valueOf(it) }

    fun getResolutionDesiredInterval(): Long =
        preferences.getString(RESOLUTION_DESIRED_INTERVAL_KEY, null)?.toLong()
            ?: DEFAULT_RESULTION_DESIRED_INTERVAL

    fun getResolutionMinimumDisplacement(): Float =
        preferences.getString(RESOLUTION_MINIMUM_DISPLACEMENT_KEY, null)?.toFloat()
            ?: DEFAULT_RESULTION_MINIMUM_DISPLACEMENT

    fun shouldSendRawLocations() =
        preferences.getBoolean(SEND_RAW_LOCATIONS_KEY, DEFAULT_SEND_RAW_LOCATIONS)

    fun shouldSendResolution() =
        preferences.getBoolean(SEND_RESOLUTION_KEY, DEFAULT_SEND_RESOLUTION)

    fun isConstantLocationEngineResolutionEnabled() = preferences.getBoolean(
        ENABLE_CONSTANT_LOCATION_ENGINE_RESOLUTION_KEY,
        DEFAULT_ENABLE_CONSTANT_LOCATION_ENGINE_RESOLUTION
    )

    fun getConstantLocationEngineResolution(): Resolution {
        val accuracy = preferences.getString(CONSTANT_RESOLUTION_ACCURACY_KEY, DEFAULT_RESULTION_ACCURACY)!!.let {
            Accuracy.valueOf(it)
        }
        val desiredInterval = preferences.getString(CONSTANT_RESOLUTION_DESIRED_INTERVAL_KEY, null)?.toLong()
            ?: DEFAULT_RESULTION_DESIRED_INTERVAL
        val minimumDisplacement = preferences.getString(CONSTANT_RESOLUTION_MINIMUM_DISPLACEMENT_KEY, null)?.toFloat()
            ?: DEFAULT_RESULTION_MINIMUM_DISPLACEMENT
        return Resolution(accuracy, desiredInterval, minimumDisplacement.toDouble())
    }
}
