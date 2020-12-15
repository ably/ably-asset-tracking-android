package com.ably.tracking.subscriber

import android.location.Location
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution

typealias LocationUpdatedListener = (Location) -> Unit
typealias StatusListener = (Boolean) -> Unit

/**
 * Represents a subscriber. Subscribers maintain the Ably connection, relaying location updates for a tracked item back
 * to the local application as they are received from the remote publisher.
 */
interface Subscriber {
    companion object {
        /**
         * Returns the default state of the subscriber [Builder], which is incapable of starting of [Subscriber]
         * instances until it has been configured fully.
         */
        @JvmStatic
        fun subscribers(): Builder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
            // TODO return a static singleton in default state instead of creating an "empty" new one every time
            return SubscriberBuilder()
        }
    }

    /**
     * The desired resolution of updates, to be requested from the remote publisher.
     *
     * May be defined from the outset of a [Subscriber]'s lifespan by using the [resolution][Builder.resolution] method
     * on the [Builder] instance used to [start][Builder.start] it, in which case this property will return that value
     * until changed here.
     *
     * Changes to this property will take time to propogate back to the publisher, however the value returned by this
     * property will always be the most recently requested value, even if it's yet to be sent to or actioned by the
     * remote publisher.
     */
    var resolution: Resolution?

    /**
     * Stops this subscriber from listening to published locations. Once a subscriber has been stopped, it cannot be
     * restarted.
     *
     * It is strongly suggested to call this method from the main thread.
     */
    fun stop()

    /**
     * The methods implemented by builders capable of starting [Subscriber] instances.
     *
     * All methods except [start] return a new [Builder] instance, being a copy of this instance but with the
     * relevant property mutated.
     *
     * The starting point is always the default builder state, returned by the static [subscribers] method.
     */
    interface Builder {
        /**
         * Sets the Ably connection configuration.
         *
         * @param configuration The configuration to be used for Ably connection.
         * @return A new instance of the builder with this property changed.
         */
        fun connection(configuration: ConnectionConfiguration): Builder

        /**
         * Sets the logging configuration.
         *
         * @param configuration Logging configuration object [LogConfiguration]
         * @return A new instance of the builder with this property changed.
         */
        fun log(configuration: LogConfiguration): Builder

        /**
         * Sets the listener to be notified when a raw location update is available.
         *
         * @param listener The listening function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        fun rawLocationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets the listener to be notified when an enhanced location update is available.
         *
         * @param listener The listening function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        fun enhancedLocationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets the desired resolution of updates, to be requested from the remote publisher.
         *
         * @param resolution An indication of how often to this subscriber would like the publisher to sample locations,
         * at what level of positional accuracy, and how often to send them back.
         * @return A new instance of the builder with this property changed.
         */
        fun resolution(resolution: Resolution): Builder

        /**
         * Sets the asset to be tracked, using its unique tracking identifier.
         *
         * @param trackingId The unique tracking identifier for the asset.
         * @return A new instance of the builder with this property changed.
         */
        fun trackingId(trackingId: String): Builder

        /**
         * Sets the listener to be notified when the online status of the asset changes.
         *
         * @param listener the listening function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        fun assetStatusListener(listener: StatusListener): Builder

        /**
         * Creates a [Subscriber] and starts listening for location updates.
         *
         * It is strongly suggested to call this method from the main thread.
         *
         * @return A new subscriber instance.
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         */
        fun start(): Subscriber
    }
}
