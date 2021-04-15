package com.ably.tracking.subscriber

import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.connection.AuthenticationConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.common.DefaultAbly

internal data class SubscriberBuilder(
    val authenticationConfiguration: AuthenticationConfiguration? = null,
    val resolution: Resolution? = null,
    val trackingId: String? = null
) : Subscriber.Builder {

    override fun connection(configuration: AuthenticationConfiguration): Subscriber.Builder =
        this.copy(authenticationConfiguration = configuration)

    override fun resolution(resolution: Resolution): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override suspend fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            DefaultAbly(authenticationConfiguration!!),
            resolution,
            trackingId!!
        ).apply {
            start()
        }
    }

    private fun isMissingRequiredFields() =
        authenticationConfiguration == null ||
            trackingId == null
}
