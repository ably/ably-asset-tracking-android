package com.ably.tracking.subscriber

import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution

internal data class SubscriberBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val resolution: Resolution? = null,
    val trackingId: String? = null
) : Subscriber.Builder {

    override fun connection(configuration: ConnectionConfiguration): Subscriber.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun resolution(resolution: Resolution): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            DefaultAbly(connectionConfiguration!!, trackingId!!),
            resolution
        )
    }

    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            trackingId == null
}
