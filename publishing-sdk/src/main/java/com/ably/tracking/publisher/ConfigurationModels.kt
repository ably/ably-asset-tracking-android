package com.ably.tracking.publisher

import android.app.Notification
import com.ably.tracking.Resolution

data class MapConfiguration(val apiKey: String)

/**
 * Provides the notification that will be used for the background tracking service.
 */
interface PublisherNotificationProvider {
    /**
     * Returns the notification that will be displayed. This method can be called multiple times.
     */
    fun getNotification(): Notification
}

/**
 * Defines the strategy by which the various [ResolutionRequest]s and preferences are translated by [Publisher]
 * instances into a target [Resolution].
 */
interface ResolutionPolicy {

    /**
     * Determine a target [Resolution] for a [Trackable] object.
     *
     * The intention is for the resulting [Resolution] to impact networking per [Trackable].
     */
    fun resolve(request: TrackableResolutionRequest): Resolution

    /**
     * Determine a target [Resolution] from a set of resolutions.
     *
     * This set may be empty.
     *
     * The intention use for this method is to be applied to Resolutions returned by first overload
     * of [resolve] and to determine out of different resolutions per [Trackable] which [Resolution]
     * should be used for setting the location engine updates frequency.
     */
    fun resolve(resolutions: Set<Resolution>): Resolution
}

/**
 * A request for a tracking [Resolution] for a [Trackable] object.
 */
data class TrackableResolutionRequest(
    /**
     * The [Trackable] object that holds optional constraints.
     */
    val trackable: Trackable,
    /**
     * Remote [Resolution] requests for the [Trackable] object.
     *
     * This set may be empty.
     */
    val remoteRequests: Set<Resolution>
)

/**
 * Represents the destination of a [Trackable].
 */
data class Destination(
    /**
     * Latitude of the location in degrees.
     */
    val latitude: Double,
    /**
     * Longitude of the location in degrees.
     */
    val longitude: Double,
)

/**
 * Represents an asset that is tracked by the [Publisher].
 */
data class Trackable(
    /**
     * The unique identifier of the asset. It is used by the Subscriber SDK to receive location updates for the asset.
     */
    val id: String,
    /**
     * The destination of the asset.
     */
    val destination: Destination? = null,
    /**
     * A set of constraints used to determine the final [Resolution] for the asset.
     */
    val constraints: ResolutionConstraints? = null
) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            null -> false
            is Trackable -> other.id == id
            else -> false
        }

    override fun hashCode(): Int = id.hashCode()
}

data class Subscriber(val id: String, val trackable: Trackable)

sealed class Proximity

/**
 * A proximity where there is the capability to specify both temporal and spatial elements.
 *
 * At least one of [temporal] or [spatial] **must** be provided.
 */
data class DefaultProximity(
    /**
     * Estimated time remaining to arrive at the destination, in milliseconds.
     */
    val temporal: Long?,

    /**
     * Distance from the destination, in metres.
     */
    val spatial: Double?
) : Proximity() {
    init {
        if (null == temporal && null == spatial) {
            throw NullPointerException("Both temporal and spatial may not be null. At least one must be specified.")
        }
    }

    /**
     * Create a proximity where only the temporal element is specified.
     * @param temporal Distance from the destination, in metres.
     */
    constructor(temporal: Long) : this(temporal, null)

    /**
     * Create a proximity where only the spatial element is specified.
     * @param spatial Estimated time remaining to arrive at the destination, in milliseconds.
     */
    constructor(spatial: Double) : this(null, spatial)
}

/**
 * The set of resolutions which must be defined in order to specify [DefaultResolutionConstraints], which are required
 * to use the default [ResolutionPolicy], as created by instances of the [DefaultResolutionPolicyFactory] class.
 */
data class DefaultResolutionSet(
    /**
     * The resolution to select if above the [proximityThreshold][DefaultResolutionConstraints.proximityThreshold],
     * with no subscribers.
     */
    val farWithoutSubscriber: Resolution?,

    /**
     * The resolution to select if above the [proximityThreshold][DefaultResolutionConstraints.proximityThreshold],
     * with one or more subscribers.
     */
    val farWithSubscriber: Resolution?,

    /**
     * The resolution to select if below the [proximityThreshold][DefaultResolutionConstraints.proximityThreshold],
     * with no subscribers.
     */
    val nearWithoutSubscriber: Resolution?,

    /**
     * The resolution to select if below the [proximityThreshold][DefaultResolutionConstraints.proximityThreshold],
     * with one or more subscribers.
     */
    val nearWithSubscriber: Resolution?
) {
    /**
     * Creates an instance of this class, using a single [Resolution] for all states.
     *
     * @param resolution The resolution to be used to populate all fields.
     */
    constructor(resolution: Resolution) : this(resolution, resolution, resolution, resolution)

    init {
        if (farWithSubscriber == null && farWithoutSubscriber == null && nearWithSubscriber == null && nearWithoutSubscriber == null) {
            throw NullPointerException("All resolutions may not be null. At least one must be specified.")
        }
    }
}

internal fun DefaultResolutionSet.getResolution(isNear: Boolean, hasSubscriber: Boolean): Resolution? = when {
    isNear && hasSubscriber -> nearWithSubscriber
    isNear && !hasSubscriber -> nearWithoutSubscriber
    !isNear && hasSubscriber -> farWithSubscriber
    else -> farWithoutSubscriber
}

/**
 * Specifies factors which contribute towards deciding the tracking [Resolution] for a [Trackable].
 */
interface ResolutionConstraints

/**
 * Specifies the thresholds and corresponding logical mappings for a [Trackable] that can be used
 * to calculate its [Resolution] by a [ResolutionPolicy]. [ResolutionConstraints] is an optional
 * part of the [Trackable] and is not required by the [DefaultResolutionPolicy] to calculate a resolution.
 * However, if it is provided the calculated resolution can be better adjusted to the current situation.
 */
data class DefaultResolutionConstraints(
    /**
     * Tracking [Resolution] specifications which are to be used according to thresholds.
     */
    val resolutions: DefaultResolutionSet,

    /**
     * The boundary differentiating between "near" and "far" in [resolutions].
     */
    val proximityThreshold: Proximity,

    /**
     * In the range 0.0f (no battery) to 100.0f (full battery).
     */
    val batteryLevelThreshold: Float,

    /**
     * The multiplier to be applied to the [interval][Resolution.desiredInterval] when the battery level is below
     * [batteryLevelThreshold].
     */
    val lowBatteryMultiplier: Float
) : ResolutionConstraints

/**
 * Represents the means of transport that's being used.
 */
enum class RoutingProfile {
    /**
     * For car and motorcycle routing. This profile prefers high-speed roads like highways.
     */
    DRIVING,

    /**
     * For bicycle routing. This profile prefers routes that are safe for cyclist, avoiding highways and preferring streets with bike lanes.
     */
    CYCLING,

    /**
     * For pedestrian and hiking routing. This profile prefers sidewalks and trails.
     */
    WALKING,

    /**
     * For car and motorcycle routing. This profile factors in current and historic traffic conditions to avoid slowdowns.
     */
    DRIVING_TRAFFIC,
}

sealed class LocationSource
class LocationSourceAbly private constructor(val simulationChannelName: String) : LocationSource() {
    companion object {
        @JvmStatic
        fun create(simulationChannelName: String) = LocationSourceAbly(simulationChannelName)
    }

    private constructor() : this("")
}

class LocationSourceRaw private constructor(
    val historyData: LocationHistoryData,
    val onDataEnded: (() -> Unit)? = null
) :
    LocationSource() {
    companion object {
        @JvmSynthetic
        fun create(historyData: LocationHistoryData, onDataEnded: (() -> Unit)? = null) =
            LocationSourceRaw(historyData, onDataEnded)

        @JvmStatic
        fun createRaw(historyData: LocationHistoryData, callback: (DataEndedCallback)? = null) =
            LocationSourceRaw(historyData, callback)
    }

    private constructor() : this(historyData = LocationHistoryData(emptyList()), onDataEnded = null)
    private constructor(historyData: LocationHistoryData, callback: (DataEndedCallback)? = null) : this(
        historyData,
        { callback?.onDataEnded() }
    )
}

interface DataEndedCallback {
    fun onDataEnded()
}

/**
 * Represents the vehicle that's used by the publisher.
 */
enum class VehicleProfile {
    /**
     * For cars and motorcycles.
     */
    CAR,

    /**
     * For bicycles and pedestrians.
     */
    BICYCLE,
}
