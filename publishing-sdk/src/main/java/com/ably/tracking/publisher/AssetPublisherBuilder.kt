package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission

internal data class AssetPublisherBuilder(
    val ablyConfiguration: AblyConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val batteryConfiguration: BatteryConfiguration? = null,
    val debugConfiguration: DebugConfiguration? = null,
    val assetMetadataJson: String? = null,
    val tripMetadataJson: String? = null,
    val locationUpdatedListener: LocationUpdatedListener? = null,
    val androidContext: Context? = null,
    val trackingId: String? = null,
    val destination: String? = null,
    val vehicleType: String? = null
) : AssetPublisher.Builder {

    override fun ablyConfig(configuration: AblyConfiguration): AssetPublisher.Builder =
        this.copy(ablyConfiguration = configuration)

    override fun mapConfig(configuration: MapConfiguration): AssetPublisher.Builder =
        this.copy(mapConfiguration = configuration)

    override fun logConfig(configuration: LogConfiguration): AssetPublisher.Builder =
        this.copy(logConfiguration = configuration)

    override fun batteryConfig(configuration: BatteryConfiguration): AssetPublisher.Builder =
        this.copy(batteryConfiguration = configuration)

    override fun debugConfig(configuration: DebugConfiguration?): AssetPublisher.Builder =
        this.copy(debugConfiguration = configuration)

    override fun assetMetadataJson(metadataJsonString: String): AssetPublisher.Builder =
        this.copy(assetMetadataJson = metadataJsonString)

    override fun tripMetadataJson(metadataJsonString: String): AssetPublisher.Builder =
        this.copy(tripMetadataJson = metadataJsonString)

    override fun locationUpdatedListener(listener: LocationUpdatedListener): AssetPublisher.Builder =
        this.copy(locationUpdatedListener = listener)

    override fun androidContext(context: Context): AssetPublisher.Builder =
        this.copy(androidContext = context)

    override fun delivery(
        trackingId: String,
        destination: String,
        vehicleType: String
    ): AssetPublisher.Builder =
        this.copy(trackingId = trackingId, destination = destination, vehicleType = vehicleType)

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): AssetPublisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultAssetPublisher(
            ablyConfiguration!!,
            mapConfiguration!!,
            debugConfiguration,
            trackingId!!,
            locationUpdatedListener!!,
            androidContext!!
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create AssetPublisher)
    private fun isMissingRequiredFields() =
        ablyConfiguration == null ||
            mapConfiguration == null ||
            locationUpdatedListener == null ||
            androidContext == null ||
            trackingId == null
}
