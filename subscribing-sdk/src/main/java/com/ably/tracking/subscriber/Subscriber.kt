package com.ably.tracking.subscriber

import android.location.Location
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution

typealias LocationUpdatedListener = (Location) -> Unit
typealias StatusListener = (Boolean) -> Unit

interface Subscriber {
    companion object {
        /**
         * Returns the default builder of Subscriber instances.
         */
        @JvmStatic
        fun subscribers(): Builder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
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
     * The [onSuccess] callback will be called once the request has been successfully registered with the server,
     * however this does not necessarily mean that the request has been received and actioned by the publisher.
     *
     * @param resolution The resolution to request.
     * @param onSuccess Function to be called if the request was successfully registered with the server.
     * @param onError Function to be called if the request could not be sent or it was not possible to confirm that the
     * server had processed the request.
     */
    fun sendChangeRequest(resolution: Resolution, onSuccess: () -> Unit, onError: (Exception) -> Unit)

    /**
     * Stops asset subscriber from listening for asset location
     *
     * It is strongly suggested to call this method from the main thread.
     */
    fun stop()

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
         * @return A new instance of the builder with logging configuration changed
         */
        fun log(configuration: LogConfiguration): Builder

        /**
         * Sets listener that notifies about raw location updates
         *
         * @param listener Listener function that takes updated [Location] and returns nothing
         * @return A new instance of the builder with raw location updates listener changed
         */
        fun rawLocationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets listener that notifies about enhanced location updates
         *
         * @param listener Listener function that takes updated [Location] and returns nothing
         * @return A new instance of the builder with enhanced location updates listener changed
         */
        fun enhancedLocationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets the desired resolution of updates, to be requested from the remote publisher.
         *
         * @param resolution An indication of how often to this subscriber would like the publisher to sample locations,
         * at what level of positional accuracy, and how often to send them back.
         * @return A new instance of the builder with resolution changed
         */
        fun resolution(resolution: Resolution): Builder

        /**
         * Sets tracking ID of the tracked asset
         *
         * @param trackingId ID of the tracked asset
         * @return A new instance of the builder with tracking ID changed
         */
        fun trackingId(trackingId: String): Builder

        /**
         * Sets asset status listener for checking if asset is online
         *
         * @param listener Listener function that takes [Boolean] that's true when asset is online
         * @return A new instance of the builder with this field changed
         */
        fun assetStatusListener(listener: StatusListener): Builder

        /**
         * Creates an [Subscriber] and starts subscribing to the asset location
         *
         * It is strongly suggested to call this method from the main thread.
         *
         * @return A new instance of [Subscriber]
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         */
        fun start(): Subscriber
    }
}
