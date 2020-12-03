package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.AblyConfiguration
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.LogConfiguration

internal data class PublisherBuilder(
    val ablyConfiguration: AblyConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val debugConfiguration: DebugConfiguration? = null,
    val locationUpdatedListener: LocationUpdatedListener? = null,
    val androidContext: Context? = null,
    val transportationMode: TransportationMode? = null,
    val resolutionPolicy: ResolutionPolicy? = null
) : Publisher.Builder {

    override fun ably(configuration: AblyConfiguration): Publisher.Builder =
        this.copy(ablyConfiguration = configuration)

    override fun map(configuration: MapConfiguration): Publisher.Builder =
        this.copy(mapConfiguration = configuration)

    override fun log(configuration: LogConfiguration): Publisher.Builder =
        this.copy(logConfiguration = configuration)

    override fun debug(configuration: DebugConfiguration?): Publisher.Builder =
        this.copy(debugConfiguration = configuration)

    override fun locationUpdatedListener(listener: LocationUpdatedListener): Publisher.Builder =
        this.copy(locationUpdatedListener = listener)

    override fun androidContext(context: Context): Publisher.Builder =
        this.copy(androidContext = context)

    override fun mode(mode: TransportationMode): Publisher.Builder =
        this.copy(transportationMode = mode)

    override fun resolutionPolicy(policy: ResolutionPolicy): Publisher.Builder =
        this.copy(resolutionPolicy = policy)

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): Publisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultPublisher(
            ablyConfiguration!!,
            mapConfiguration!!,
            debugConfiguration,
            locationUpdatedListener!!,
            androidContext!!
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create Publisher)
    private fun isMissingRequiredFields() =
        ablyConfiguration == null ||
            mapConfiguration == null ||
            locationUpdatedListener == null ||
            androidContext == null ||
            transportationMode == null
}
