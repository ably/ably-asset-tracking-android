package com.ably.tracking.subscriber

import com.ably.tracking.AssetStatusListener
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LocationUpdatedListener
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution

internal data class SubscriberBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val rawLocationUpdatedListener: LocationUpdatedListener? = null,
    val enhancedLocationUpdatedListener: LocationUpdatedListener? = null,
    val resolution: Resolution? = null,
    val trackingId: String? = null,
    val assetStatusListener: AssetStatusListener? = null
) : Subscriber.Builder {

    override fun connection(configuration: ConnectionConfiguration): Subscriber.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun log(configuration: LogConfiguration): Subscriber.Builder =
        this.copy(logConfiguration = configuration)

    override fun rawLocationUpdatedListener(listener: LocationUpdatedListener): Subscriber.Builder =
        this.copy(rawLocationUpdatedListener = listener)

    override fun enhancedLocationUpdatedListener(listener: LocationUpdatedListener): Subscriber.Builder =
        this.copy(enhancedLocationUpdatedListener = listener)

    override fun resolution(resolution: Resolution): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun assetStatusListener(listener: AssetStatusListener): Subscriber.Builder =
        this.copy(assetStatusListener = listener)

    override fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            connectionConfiguration!!,
            rawLocationUpdatedListener!!,
            enhancedLocationUpdatedListener!!,
            trackingId!!,
            assetStatusListener,
            resolution
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create AssetSubscriber)
    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            rawLocationUpdatedListener == null ||
            enhancedLocationUpdatedListener == null ||
            trackingId == null
}
