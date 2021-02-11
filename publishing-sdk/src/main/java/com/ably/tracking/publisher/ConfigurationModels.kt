package com.ably.tracking.publisher

import com.ably.tracking.Resolution

data class MapConfiguration(val apiKey: String)

/**
 * Defines the strategy by which the various [ResolutionRequest]s and preferences are translated by [Publisher]
 * instances into a target [Resolution].
 */
interface ResolutionPolicy {
    /**
     * Defines the methods which can be called by a resolution policy at any time.
     */
    interface Methods {
        /**
         * Defines the methods to be implemented by proximity handlers.
         */
        interface ProximityHandler {
            /**
             * The desired proximity has been reached.
             *
             * @param threshold The threshold which was supplied when this handler was registered.
             */
            fun onProximityReached(threshold: Proximity)

            /**
             * This handler has been cancelled, either explicitly using [cancelProximityThreshold], or implicitly
             * because a new handler has taken its place for the associated [Publisher].
             */
            fun onProximityCancelled()
        }

        /**
         * Causes the current tracking [Resolution] to be evaluated again by its associated [Publisher] instance.
         *
         * The [ResolutionPolicy] instance which was provided with these [Methods] will be consulted again as soon as
         * possible after this method returns.
         */
        fun refresh()

        /**
         * Registers a handler to be called when a given proximity to the destination of the active [Trackable] object
         * has been reached.
         *
         * Proximity is considered reached when it is at or nearer the destination than the [threshold] specifies.
         *
         * A [Publisher] instance can only have one proximity threshold handler active at any one time. If there is
         * already a registered proximity handler then it will be cancelled and replaced.
         *
         * The supplied [handler] will only be called once, either:
         * - with [onProximityReached][ProximityHandler.onProximityReached] when proximity has been reached;
         * - or with [onProximityCancelled][ProximityHandler.onProximityCancelled] when cancelled (either
         * [explicitly][cancelProximityThreshold] or implicitly because it was replaced by a subsequent call to this
         * method)
         *
         * @param threshold The threshold at which to call the handler.
         * @param handler The handler whose [onProximityReached][ProximityHandler.onProximityReached] method is to be
         * called when this threshold proximity has been reached.
         */
        fun setProximityThreshold(threshold: Proximity, handler: ProximityHandler)

        /**
         * Removes the currently registered proximity handler, if there is one. Otherwise does nothing.
         */
        fun cancelProximityThreshold()
    }

    /**
     * Defines the methods which can be called by a resolution policy when it is created.
     *
     * Methods on this interface may only be called from within implementations of
     * [createResolutionPolicy][Factory.createResolutionPolicy].
     */
    interface Hooks {
        /**
         * A handler of events relating to the addition, removal and activation of [Trackable] objects for a
         * [Publisher] instance.
         */
        interface TrackableSetListener {
            /**
             * A [Trackable] object has been added to the [Publisher]'s set of tracked objects.
             *
             * If the operation adding [trackable] is also making it the [actively][Publisher.active] tracked object
             * then [onActiveTrackableChanged] will subsequently be called.
             *
             * @param trackable The object which has been added to the tracked set.
             */
            fun onTrackableAdded(trackable: Trackable)

            /**
             * A [Trackable] object has been removed from the [Publisher]'s set of tracked objects.
             *
             * If [trackable] was the [actively][Publisher.active] tracked object then [onActiveTrackableChanged] will
             * subsequently be called.
             *
             * @param trackable The object which has been removed from the tracked set.
             */
            fun onTrackableRemoved(trackable: Trackable)

            /**
             * The [actively][Publisher.active] tracked object has changed.
             *
             * @param trackable The object, from the tracked set, which has been activated - or no value if there is
             * no longer an actively tracked object.
             */
            fun onActiveTrackableChanged(trackable: Trackable?)
        }

        /**
         * A handler of events relating to the addition or removal of remote [Subscriber]s to a [Publisher] instance.
         */
        interface SubscriberSetListener {
            /**
             * A [Subscriber] has subscribed to receive updates for one or more [Trackable] objects from the
             * [Publisher]'s set of tracked objects.
             *
             * @param subscriber The remote entity that subscribed.
             */
            fun onSubscriberAdded(subscriber: Subscriber)

            /**
             * A [Subscriber] has unsubscribed from updates for one or more [Trackable] objects from the [Publisher]'s
             * set of tracked objects.
             *
             * @param subscriber The remote entity that unsubscribed.
             */
            fun onSubscriberRemoved(subscriber: Subscriber)
        }

        /**
         * Register a handler for the addition, removal and activation of [Trackable] objects for the [Publisher]
         * instance whose [creation][Publisher.Builder.start] caused
         * [createResolutionPolicy][Factory.createResolutionPolicy] to be called.
         *
         * This method should only be called once within the scope of creation of a single publisher's resolution
         * policy. Subsequent calls to this method will replace the previous handler.
         *
         * @param listener The handler, which may be called multiple times during the lifespan of the publisher.
         */
        fun trackables(listener: TrackableSetListener)

        /**
         * Register a handler for the addition and removal of remote [Subscriber]s to the [Publisher] instance whose
         * [creation][Publisher.Builder.start] caused [createResolutionPolicy][Factory.createResolutionPolicy] to be
         * called.
         *
         * This method should only be called once within the scope of creation of a single publisher's resolution
         * policy. Subsequent calls to this method will replace the previous handler.
         *
         * @param listener The handler, which may be called multiple times during the lifespan of the publisher.
         */
        fun subscribers(listener: SubscriberSetListener)
    }

    /**
     * Defines the methods to be implemented by resolution policy factories, whose responsibility it is to create
     * a new [ResolutionPolicy] instance when a [Publisher] is [started][Publisher.Builder.start].
     */
    interface Factory {
        /**
         * This method will be called once for each [Publisher] instance started by any [Builder][Publisher.Builder]
         * instance against which this [Factory] was [registered][Publisher.Builder.resolutionPolicy].
         *
         * Calling methods on [hooks] after this method has returned will throw an exception.
         *
         * Calling methods on [methods] after this method has returned is allowed and expected.
         *
         * @param hooks Methods which may be called while inside this method implementation, but not after.
         * @param methods Methods which may be called after this method has returned.
         * @return A resolution policy to be used for the lifespan of the associated [Publisher].
         */
        fun createResolutionPolicy(hooks: Hooks, methods: Methods): ResolutionPolicy
    }

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

data class Destination(
    val latitude: Double,
    val longitude: Double
)

data class Trackable(
    val id: String,
    val metadata: String? = null,
    val destination: Destination? = null,
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
sealed class ResolutionConstraints

/**
 * Specifies the thresholds and corresponding logical mappings for tracking [Resolution]s that are required by the
 * default [ResolutionPolicy], which can adopted by [Publisher] instances using the [Builder][Publisher.Builder]'s
 * [resolutionPolicy][Publisher.Builder.resolutionPolicy] method, providing it with an instance of the
 * [DefaultResolutionPolicyFactory] class.
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
     * The multipler to be applied to the [interval][Resolution.desiredInterval] when the battery level is below
     * [batteryLevelThreshold].
     */
    val lowBatteryMultiplier: Float
) : ResolutionConstraints()

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

// TODO - probably should be removed in the final version
// https://github.com/ably/ably-asset-tracking-android/issues/19
data class DebugConfiguration(
    val locationHistoryHandler: ((LocationHistoryData) -> Unit)? = null
)

sealed class LocationSource
data class LocationSourceAbly(val simulationChannelName: String) : LocationSource()
data class LocationSourceRaw(val historyData: LocationHistoryData, val onDataEnded: (() -> Unit)? = null) : LocationSource()
