package com.ably.tracking.subscriber

import com.ably.tracking.AssetStatusHandler
import com.ably.tracking.AssetStatusListener
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LocationHandler
import com.ably.tracking.LocationListener
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.ResultListener

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
     * Sends the desired resolution for updates, to be requested from the remote publisher.
     *
     * An initial resolution may be defined from the outset of a [Subscriber]'s lifespan by using the
     * [resolution][Builder.resolution] method on the [Builder] instance used to [start][Builder.start] it.
     *
     * Requests sent using this method will take time to propagate back to the publisher.
     *
     * The [handler] will be called once the request has been successfully registered with the server,
     * however this does not necessarily mean that the request has been received and actioned by the publisher.
     *
     * This method overload is preferable when calling from Kotlin.
     *
     * @param resolution The resolution to request.
     * @param handler The function to be notified.
     */
    @JvmSynthetic
    fun sendChangeRequest(resolution: Resolution, handler: ResultHandler<Unit>)

    /**
     * Sends the desired resolution for updates, to be requested from the remote publisher.
     *
     * An initial resolution may be defined from the outset of a [Subscriber]'s lifespan by using the
     * [resolution][Builder.resolution] method on the [Builder] instance used to [start][Builder.start] it.
     *
     * Requests sent using this method will take time to propagate back to the publisher.
     *
     * The [listener] will be called once the request has been successfully registered with the server,
     * however this does not necessarily mean that the request has been received and actioned by the publisher.
     *
     * This method overload is provided for the convenience of those calling from Java.
     *
     * @param resolution The resolution to request.
     * @param listener The object to be notified.
     */
    fun sendChangeRequest(resolution: Resolution, listener: ResultListener<Void?>)

    /**
     * Stops this subscriber from listening to published locations. Once a subscriber has been stopped, it cannot be
     * restarted.
     *
     * This method overload is preferable when calling from Kotlin.
     *
     * @param handler Called when the publisher has been successfully removed or an error occurs.
     */
    @JvmSynthetic
    fun stop(handler: ResultHandler<Unit>)

    /**
     * This method overload is provided for the convenient of those calling from Java.
     */
    fun stop(listener: ResultListener<Void?>)

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
         * Sets the handler to be notified when a raw location update is available.
         *
         * This method overload is preferable when calling from Kotlin.
         *
         * @param handler The function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        @JvmSynthetic
        fun enhancedLocations(handler: LocationHandler): Builder

        /**
         * Sets the handler to be notified when an enhanced location update is available.
         *
         * This method overload is provided for the convenience of those calling from Java.
         *
         * @param listener The listening function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        fun enhancedLocations(listener: LocationListener): Builder

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
         * Sets the handler to be notified when the online status of the asset changes.
         *
         * This method overload is preferable when calling from Kotlin.
         *
         * @param handler The function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        @JvmSynthetic
        fun assetStatus(handler: AssetStatusHandler): Builder

        /**
         * Sets the handler to be notified when the online status of the asset changes.
         *
         * This method overload is provided for the convenience of those calling from Java.
         *
         * @param listener the listening function to be notified.
         * @return A new instance of the builder with this property changed.
         */
        fun assetStatus(listener: AssetStatusListener): Builder

        /**
         * Creates a [Subscriber] and starts listening for location updates.
         *
         * @return A new subscriber instance.
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         */
        fun start(): Subscriber
    }
}
