package com.ably.tracking.subscriber

import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.Resolution
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.DefaultAblySdkRealtimeFactory
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler

internal data class SubscriberBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val resolution: Resolution? = null,
    val logHandler: LogHandler? = null,
    val trackingId: String? = null,
) : Subscriber.Builder {
    private val TAG = createLoggingTag(this)

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
            logHandler?.v("$TAG Creating a subscriber instance failed due to missing required fields")
            throw BuilderConfigurationIncompleteException()
        }
        logHandler?.v("$TAG Creating a subscriber instance")
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultSubscriber(
            DefaultAbly(DefaultAblySdkRealtimeFactory(), connectionConfiguration!!, logHandler),
            resolution,
            trackingId!!,
            logHandler,
        ).apply {
            start()
        }
    }

    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            trackingId == null
}
