package com.ably.tracking

import com.google.gson.annotations.SerializedName

data class ConnectionConfiguration(val apiKey: String, val clientId: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

/**
 * The accuracy of a geographical coordinate.
 *
 * Presents a unified representation of location accuracy (Apple) and quality priority (Android).
 */
enum class Accuracy(val level: Int) {
    /**
     * - Android: [PRIORITY_NO_POWER](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#PRIORITY_NO_POWER)
     *   (best possible with zero additional power consumption)
     * - Apple: [kCLLocationAccuracyReduced](https://developer.apple.com/documentation/corelocation/kcllocationaccuracyreduced)
     *   (preserves the user’s country, typically preserves the city, and is usually within 1–20 kilometers of the actual location)
     */
    MINIMUM(1),

    /**
     * - Android: [PRIORITY_LOW_POWER](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#PRIORITY_LOW_POWER)
     *   (coarse "city" level, circa 10km accuracy)
     * - Apple: Either [kCLLocationAccuracyKilometer](https://developer.apple.com/documentation/corelocation/kcllocationaccuracykilometer)
     *   or [kCLLocationAccuracyThreeKilometers](https://developer.apple.com/documentation/corelocation/kcllocationaccuracythreekilometers)
     */
    LOW(2),

    /**
     * - Android: [PRIORITY_BALANCED_POWER_ACCURACY](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#PRIORITY_BALANCED_POWER_ACCURACY)
     *   (coarse "block" level, circa 100m accuracy)
     * - Apple: Either [kCLLocationAccuracyNearestTenMeters](https://developer.apple.com/documentation/corelocation/kcllocationaccuracynearesttenmeters)
     *   or [kCLLocationAccuracyHundredMeters](https://developer.apple.com/documentation/corelocation/kcllocationaccuracyhundredmeters)
     */
    BALANCED(3),

    /**
     * - Android: [PRIORITY_HIGH_ACCURACY](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#PRIORITY_HIGH_ACCURACY)
     *   (most accurate locations that are available)
     * - Apple: [kCLLocationAccuracyBest](https://developer.apple.com/documentation/corelocation/kcllocationaccuracybest)
     *   (very high accuracy but not to the same level required for navigation apps)
     */
    HIGH(4),

    /**
     * - Android: same as [HIGH]
     * - Apple: [kCLLocationAccuracyBestForNavigation](https://developer.apple.com/documentation/corelocation/kcllocationaccuracybestfornavigation)
     *   (precise position information required at all times, with significant extra power requirement implication)
     */
    MAXIMUM(5),
}

/**
 * Governs how often to sample locations, at what level of positional accuracy, and how often to send them to
 * subscribers.
 */
data class Resolution(
    /**
     * The general priority for accuracy of location updates, used to govern any trade-off between power usage and
     * positional accuracy.
     *
     * The highest positional accuracy will be achieved by specifying [Accuracy.MAXIMUM], but at the expense of
     * significantly increased power usage. Conversely, the lowest power usage will be achieved by specifying
     * [Accuracy.MINIMUM] but at the expense of significantly decreased positional accuracy.
     */
    @SerializedName("accuracy")
    val accuracy: Accuracy,

    /**
     * Desired time between updates, in milliseconds. Lowering this value increases the temporal resolution.
     *
     * Location updates whose timestamp differs from the last captured update timestamp by less that this value are to
     * be filtered out.
     *
     * Used to govern the frequency of updates requested from the underlying location provider, as well as the frequency
     * of messages broadcast to subscribers.
     */
    @SerializedName("desiredInterval")
    val desiredInterval: Long,

    /**
     * Minimum positional granularity required, in metres. Lowering this value increases the spatial resolution.
     *
     * Location updates whose position differs from the last known position by a distance smaller than this value are to
     * be filtered out.
     *
     * Used to configure the underlying location provider, as well as to filter the broadcast of updates to subscribers.
     */
    @SerializedName("minimumDisplacement")
    val minimumDisplacement: Double
)
