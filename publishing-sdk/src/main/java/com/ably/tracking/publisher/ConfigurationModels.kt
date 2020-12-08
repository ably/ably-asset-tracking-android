package com.ably.tracking.publisher

import com.ably.tracking.Resolution
import io.ably.lib.realtime.ConnectionStateListener

// TODO: make sure all this works from Java user perspective

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
        interface ProximityHandler {
            fun onProximityReached(proximity: Proximity)
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
         * A [Publisher] instance can only have one proximity threshold handler active at any one time.
         */
        fun setProximityThreshold(threshold: Proximity, handler: ProximityHandler)

        fun cancelProximityThreshold()
    }

    /**
     * Defines the methods which can be called by a resolution policy when it is created.
     *
     * Methods on this interface may only be called from within implementations of
     * [createResolutionPolicy][Factory.createResolutionPolicy].
     */
    interface Hooks {
        interface TrackableSetListener {
            fun onTrackableAdded(trackable: Trackable)
            fun onTrackableRemoved(trackable: Trackable)
            fun onActiveTrackableChanged(trackable: Trackable?)
        }

        interface SubscriberSetListener {
            fun onSubscriberAdded(subscriber: Subscriber)
            fun onSubscriberRemoved(subscriber: Subscriber)
        }

        fun trackables(listener: TrackableSetListener)

        fun subscribers(listener: SubscriberSetListener)
    }

    /**
     * Defines the methods to be implemented by resolution policy factories, whose responsibility it is to create
     * a new [ResolutionPolicy] instance when a [Publisher] is started.
     */
    interface Factory {
        /**
         * Calling methods on [hooks] after this method has returned will throw an exception.
         *
         * Calling methods on [methods] after this method has returned is allowed and expected.
         */
        fun createResolutionPolicy(hooks: Hooks, methods: Methods): ResolutionPolicy
    }

    /**
     * Determine a target resolution from a set of requested resolutions.
     *
     * The set of requested resolutions may be empty.
     */
    fun resolve(requests: Set<ResolutionRequest>): Resolution
}

/**
 * A request for a tracking [Resolution] for a [Trackable] object, where the request [Origin] is known.
 */
interface ResolutionRequest {
    /**
     * The source of a [resolution] request for a [trackable] object.
     */
    enum class Origin {
        /**
         * Configured by the local application.
         */
        LOCAL,

        /**
         * Received from a remote application, where that remote application is a subscriber.
         */
        SUBSCRIBER,
    }

    /**
     * The resolution being requested.
     */
    val resolution: Resolution

    /**
     * The object being tracked.
     */
    val trackable: Trackable

    /**
     * The source of the request.
     */
    val origin: Origin
}

data class Destination(
    val latitude: Double,
    val longitude: Double
)

data class Trackable(
    val id: String,
    val metadata: String? = null,
    val destination: Destination? = null,
    val resolution: Resolution? = null
)

data class Subscriber(val id: String)

sealed class Proximity
data class SpatialProximity(val distance: Double) : Proximity()
data class TemporalProximity(val time: Long) : Proximity()

data class TransportationMode(val TBC: String)

// TODO - probably should be removed in the final version
// https://github.com/ably/ably-asset-tracking-android/issues/19
data class DebugConfiguration(
    val ablyStateChangeListener: ((ConnectionStateListener.ConnectionStateChange) -> Unit)? = null,
    val locationSource: LocationSource? = null,
    val locationHistoryReadyListener: ((String) -> Unit)? = null
)

sealed class LocationSource
data class LocationSourceAbly(val simulationChannelName: String) : LocationSource()
data class LocationSourceRaw(val historyData: String) : LocationSource()
