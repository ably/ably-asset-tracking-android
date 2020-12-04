package com.ably.tracking.subscriber

import com.ably.tracking.AblyConfiguration
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.LogConfiguration

internal data class SubscriberBuilder(
    val ablyConfiguration: AblyConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val rawLocationUpdatedListener: LocationUpdatedListener? = null,
    val enhancedLocationUpdatedListener: LocationUpdatedListener? = null,
    val resolution: Double? = null,
    val trackingId: String? = null,
    val assetStatusListener: StatusListener? = null
) : Subscriber.Builder {

    override fun ablyConfig(configuration: AblyConfiguration): Subscriber.Builder =
        this.copy(ablyConfiguration = configuration)

    override fun logConfig(configuration: LogConfiguration): Subscriber.Builder =
        this.copy(logConfiguration = configuration)

    override fun rawLocationUpdatedListener(listener: LocationUpdatedListener): Subscriber.Builder =
        this.copy(rawLocationUpdatedListener = listener)

    override fun enhancedLocationUpdatedListener(listener: LocationUpdatedListener): Subscriber.Builder =
        this.copy(enhancedLocationUpdatedListener = listener)

    override fun resolution(resolution: Double): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun assetStatusListener(listener: (Boolean) -> Unit): Subscriber.Builder =
        this.copy(assetStatusListener = listener)

    override fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            ablyConfiguration!!,
            rawLocationUpdatedListener!!,
            enhancedLocationUpdatedListener!!,
            trackingId!!,
            assetStatusListener
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create AssetSubscriber)
    private fun isMissingRequiredFields() =
        ablyConfiguration == null ||
            rawLocationUpdatedListener == null ||
            enhancedLocationUpdatedListener == null ||
            trackingId == null
}
