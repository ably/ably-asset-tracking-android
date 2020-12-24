package com.ably.tracking.subscriber

import com.ably.tracking.AssetStatusHandler
import com.ably.tracking.AssetStatusListener
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LocationHandler
import com.ably.tracking.LocationListener
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution

internal data class SubscriberBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val rawLocationHandler: LocationHandler? = null,
    val enhancedLocationHandler: LocationHandler? = null,
    val resolution: Resolution? = null,
    val trackingId: String? = null,
    val assetStatusHandler: AssetStatusHandler? = null
) : Subscriber.Builder {

    override fun connection(configuration: ConnectionConfiguration): Subscriber.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun log(configuration: LogConfiguration): Subscriber.Builder =
        this.copy(logConfiguration = configuration)

    override fun rawLocations(handler: LocationHandler): Subscriber.Builder =
        this.copy(rawLocationHandler = handler)

    override fun rawLocations(listener: LocationListener): Subscriber.Builder =
        rawLocations({ listener.onLocationUpdated(it) })

    override fun enhancedLocations(handler: LocationHandler): Subscriber.Builder =
        this.copy(enhancedLocationHandler = handler)

    override fun enhancedLocations(listener: LocationListener): Subscriber.Builder =
        enhancedLocations({ listener.onLocationUpdated(it) })

    override fun resolution(resolution: Resolution): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun assetStatus(handler: AssetStatusHandler): Subscriber.Builder =
        this.copy(assetStatusHandler = handler)

    override fun assetStatus(listener: AssetStatusListener): Subscriber.Builder =
        assetStatus({ listener.onStatusChanged(it) })

    override fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            connectionConfiguration!!,
            rawLocationHandler!!,
            enhancedLocationHandler!!,
            trackingId!!,
            assetStatusHandler,
            resolution
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create AssetSubscriber)
    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            rawLocationHandler == null ||
            enhancedLocationHandler == null ||
            trackingId == null
}
