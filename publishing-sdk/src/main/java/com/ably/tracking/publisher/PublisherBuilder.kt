package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.annotation.RequiresPermission
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.locationprovider.LocationProvider
import com.ably.tracking.locationprovider.RoutingProfile
import com.ably.tracking.logging.LogHandler

internal data class PublisherBuilder(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val locationProvider: LocationProvider? = null,
    val routingProfile: RoutingProfile = RoutingProfile.DRIVING,
    val resolutionPolicyFactory: ResolutionPolicy.Factory? = null,
    val logHandler: LogHandler? = null,
    val areRawLocationsEnabled: Boolean? = null,
) : Publisher.Builder {

    override fun connection(configuration: ConnectionConfiguration): Publisher.Builder =
        this.copy(connectionConfiguration = configuration)

    override fun locationProvider(provider: LocationProvider): Publisher.Builder =
        this.copy(locationProvider = provider)

    override fun profile(profile: RoutingProfile): Publisher.Builder =
        this.copy(routingProfile = profile)

    override fun resolutionPolicy(factory: ResolutionPolicy.Factory): Publisher.Builder =
        this.copy(resolutionPolicyFactory = factory)

    override fun logHandler(logHandler: LogHandler): Publisher.Builder =
        this.copy(logHandler = logHandler)

    override fun rawLocations(enabled: Boolean): Publisher.Builder =
        this.copy(areRawLocationsEnabled = enabled)

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): Publisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultPublisher(
            DefaultAbly(connectionConfiguration!!, logHandler),
            locationProvider!!,
            resolutionPolicyFactory!!,
            routingProfile,
            logHandler,
            areRawLocationsEnabled,
        )
    }

    private fun isMissingRequiredFields() =
        connectionConfiguration == null ||
            locationProvider == null ||
            resolutionPolicyFactory == null
}
