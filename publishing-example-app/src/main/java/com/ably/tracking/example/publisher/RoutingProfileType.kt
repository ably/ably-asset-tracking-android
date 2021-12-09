package com.ably.tracking.example.publisher

import androidx.annotation.StringRes

enum class RoutingProfileType(@StringRes val displayNameResourceId: Int) {
    DRIVING(R.string.routing_profile_driving),
    CYCLING(R.string.routing_profile_cycling),
    WALKING(R.string.routing_profile_walking),
    DRIVING_TRAFFIC(R.string.routing_profile_driving_traffic),
}
