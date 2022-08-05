package com.ably.tracking.example.publisher

import androidx.annotation.StringRes
import com.ably.tracking.publisher.VehicleProfile

enum class VehicleProfileType(@StringRes val displayNameResourceId: Int) {
    CAR(R.string.vehicle_profile_car),
    BICYCLE(R.string.vehicle_profile_bicycle),
}

fun VehicleProfileType.toAssetTracking(): VehicleProfile =
    when (this) {
        VehicleProfileType.CAR -> VehicleProfile.CAR
        VehicleProfileType.BICYCLE -> VehicleProfile.BICYCLE
    }
