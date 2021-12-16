package com.ably.tracking.example.publisher

import androidx.annotation.StringRes
import com.ably.tracking.publisher.RoutingProfile

enum class RoutingProfileType(@StringRes val displayNameResourceId: Int) {
    DRIVING(R.string.routing_profile_driving),
    CYCLING(R.string.routing_profile_cycling),
    WALKING(R.string.routing_profile_walking),
    DRIVING_TRAFFIC(R.string.routing_profile_driving_traffic),
}

fun RoutingProfileType.toAssetTracking(): RoutingProfile =
    when (this) {
        RoutingProfileType.DRIVING -> RoutingProfile.DRIVING
        RoutingProfileType.CYCLING -> RoutingProfile.CYCLING
        RoutingProfileType.WALKING -> RoutingProfile.WALKING
        RoutingProfileType.DRIVING_TRAFFIC -> RoutingProfile.DRIVING_TRAFFIC
    }
