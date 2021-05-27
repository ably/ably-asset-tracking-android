package com.ably.tracking.example.publisher

enum class LocationSourceType(val displayName: String) {
    PHONE("Phone"), ABLY_CHANNEL("Ably Channel"), S3_FILE("S3 File")
}
