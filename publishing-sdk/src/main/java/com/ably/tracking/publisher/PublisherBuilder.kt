package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler

internal data class PublisherBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val androidContext: Context? = null,
    val routingProfile: RoutingProfile = RoutingProfile.DRIVING,
    val resolutionPolicyFactory: ResolutionPolicy.Factory? = null,
    val logHandler: LogHandler? = null,
    val notification: AssetTrackingNotification? = null,
    val locationSource: LocationSource? = null
) : Publisher.Builder {

    override fun connection(configuration: ConnectionConfiguration): Publisher.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun map(configuration: MapConfiguration): Publisher.Builder =
        this.copy(mapConfiguration = configuration)

    override fun androidContext(context: Context): Publisher.Builder =
        this.copy(androidContext = context)

    override fun profile(profile: RoutingProfile): Publisher.Builder =
        this.copy(routingProfile = profile)

    override fun resolutionPolicy(factory: ResolutionPolicy.Factory): Publisher.Builder =
        this.copy(resolutionPolicyFactory = factory)

    override fun locationSource(locationSource: LocationSource?): Publisher.Builder =
        this.copy(locationSource = locationSource)

    override fun logHandler(logHandler: LogHandler): Publisher.Builder =
        this.copy(logHandler = logHandler)

    override fun notification(notification: AssetTrackingNotification): Publisher.Builder =
        this.copy(notification = notification)

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): Publisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultPublisher(
            DefaultAbly(connectionConfiguration!!, logHandler),
            DefaultMapbox(
                androidContext!!,
                mapConfiguration!!,
                connectionConfiguration,
                locationSource,
                logHandler,
                notification!!
            ),
            resolutionPolicyFactory!!,
            routingProfile,
            logHandler,
        )
    }

    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            mapConfiguration == null ||
            androidContext == null ||
            notification == null ||
            resolutionPolicyFactory == null
}
