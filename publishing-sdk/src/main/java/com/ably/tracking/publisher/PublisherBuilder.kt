package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.connection.AuthenticationConfiguration
import com.ably.tracking.common.DefaultAbly

internal data class PublisherBuilder(
    val authenticationConfiguration: AuthenticationConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val androidContext: Context? = null,
    val routingProfile: RoutingProfile = RoutingProfile.DRIVING,
    val resolutionPolicyFactory: ResolutionPolicy.Factory? = null,
    val locationSource: LocationSource? = null
) : Publisher.Builder {

    override fun connection(configuration: AuthenticationConfiguration): Publisher.Builder =
        this.copy(authenticationConfiguration = configuration)

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

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    override fun start(): Publisher {
        if (isMissingRequiredFields()) {
            throw BuilderConfigurationIncompleteException()
        }
        // All below fields are required and above code checks if they are nulls, so using !! should be safe from NPE
        return DefaultPublisher(
            DefaultAbly(authenticationConfiguration!!),
            DefaultMapbox(androidContext!!, mapConfiguration!!, authenticationConfiguration, locationSource),
            resolutionPolicyFactory!!,
            routingProfile,
            DefaultBatteryDataProvider(androidContext)
        )
    }

    private fun isMissingRequiredFields() =
        authenticationConfiguration == null ||
            mapConfiguration == null ||
            androidContext == null ||
            resolutionPolicyFactory == null
}
