package com.ably.tracking.subscriber

import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.Resolution
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.ConnectionConfiguration

internal data class SubscriberBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val resolution: Resolution? = null,
    val logHandler: LogHandler? = null,
    val trackingId: String? = null
) : Subscriber.Builder {

    override fun connection(configuration: ConnectionConfiguration): Subscriber.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun resolution(resolution: Resolution): Subscriber.Builder =
        this.copy(resolution = resolution)

    override fun trackingId(trackingId: String): Subscriber.Builder =
        this.copy(trackingId = trackingId)

    override fun logHandler(logHandler: LogHandler): Subscriber.Builder =
        this.copy(logHandler = logHandler)

    override suspend fun start(): Subscriber {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            DefaultAbly(connectionConfiguration!!),
            resolution,
            trackingId!!
        ).apply {
            start()
        }
    }

    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            trackingId == null
}
