package com.ably.tracking.example.publisher

import androidx.annotation.StringRes

enum class LocationSourceType(@StringRes val displayNameResourceId: Int) {
    PHONE(R.string.location_source_phone),
    ABLY_CHANNEL(R.string.location_source_ably_channel),
    S3_FILE(R.string.location_source_s3_file)
}
