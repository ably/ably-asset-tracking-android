package com.ably.tracking.subscriber

internal data class AssetSubscriberBuilder(
    val ablyConfiguration: AblyConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val rawLocationUpdatedListener: LocationUpdatedListener? = null,
    val enhancedLocationUpdatedListener: LocationUpdatedListener? = null,
    val resolution: Double? = null,
    val trackingId: String? = null
) : AssetSubscriber.Builder {

    override fun ablyConfig(configuration: AblyConfiguration): AssetSubscriber.Builder =
        this.copy(ablyConfiguration = configuration)

    override fun logConfig(configuration: LogConfiguration): AssetSubscriber.Builder =
        this.copy(logConfiguration = configuration)

    override fun rawLocationUpdatedListener(listener: LocationUpdatedListener): AssetSubscriber.Builder =
        this.copy(rawLocationUpdatedListener = listener)

    override fun enhancedLocationUpdatedListener(listener: LocationUpdatedListener): AssetSubscriber.Builder =
        this.copy(enhancedLocationUpdatedListener = listener)

    override fun resolution(resolution: Double): AssetSubscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): AssetSubscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun start(): AssetSubscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultAssetSubscriber(
            ablyConfiguration!!,
            rawLocationUpdatedListener!!,
            enhancedLocationUpdatedListener!!,
            trackingId!!
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create AssetSubscriber)
    private fun isMissingRequiredFields() =
        ablyConfiguration == null ||
            rawLocationUpdatedListener == null ||
            enhancedLocationUpdatedListener == null ||
            trackingId == null
}
