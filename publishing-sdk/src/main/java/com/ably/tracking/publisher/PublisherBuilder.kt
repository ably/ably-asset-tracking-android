package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LogConfiguration

internal data class PublisherBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val debugConfiguration: DebugConfiguration? = null,
    val androidContext: Context? = null,
    val routingProfile: RoutingProfile = RoutingProfile.DRIVING,
    val resolutionPolicyFactory: ResolutionPolicy.Factory? = null
) : Publisher.Builder {

    override fun connection(configuration: ConnectionConfiguration): Publisher.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun map(configuration: MapConfiguration): Publisher.Builder =
        this.copy(mapConfiguration = configuration)

    override fun log(configuration: LogConfiguration): Publisher.Builder =
        this.copy(logConfiguration = configuration)

    override fun debug(configuration: DebugConfiguration?): Publisher.Builder =
        this.copy(debugConfiguration = configuration)

    override fun androidContext(context: Context): Publisher.Builder =
        this.copy(androidContext = context)

    override fun profile(profile: RoutingProfile): Publisher.Builder =
        this.copy(routingProfile = profile)

    override fun resolutionPolicy(factory: ResolutionPolicy.Factory): Publisher.Builder =
        this.copy(resolutionPolicyFactory = factory)

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): Publisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultPublisher(
            DefaultAblyService(connectionConfiguration!!),
            connectionConfiguration,
            mapConfiguration!!,
            debugConfiguration,
            androidContext!!,
            resolutionPolicyFactory!!,
            routingProfile
        )
    }

    // TODO - define which fields are required and which are optional (for now: only fields needed to create Publisher)
    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            mapConfiguration == null ||
            androidContext == null ||
            resolutionPolicyFactory == null
}
