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
