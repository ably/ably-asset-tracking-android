package com.ably.tracking.subscriber

import android.location.Location

typealias LocationUpdatedListener = (Location) -> Unit

interface AssetSubscriber {
    companion object {
        /**
         * Returns the default builder of Subscriber instances.
         */
        @JvmStatic
        fun subscribers(): Builder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
            TODO()
        }
    }

    /**
     * Stops asset subscriber from listening for asset location
     *
     * It is strongly suggested to call this method from the main thread.
     */
    fun stop()

    interface Builder {
        /**
         * Sets the Ably configuration.
         *
         * @param configuration Ably library configuration object [AblyConfiguration]
         * @return A new instance of the builder with Ably configuration changed
         */
        fun ablyConfig(configuration: AblyConfiguration): Builder

        /**
         * Sets the logging configuration.
         *
         * @param configuration Logging configuration object [LogConfiguration]
         * @return A new instance of the builder with logging configuration changed
         */
        fun logConfig(configuration: LogConfiguration): Builder

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
         * Sets resolution of updates from the publisher
         *
         * @param resolution resolution of updates from the publisher
         * @return A new instance of the builder with resolution changed
         */
        fun resolution(resolution: Double): Builder

        /**
         * Sets tracking ID of the tracked asset
         *
         * @param trackingId ID of the tracked asset
         * @return A new instance of the builder with tracking ID changed
         */
        fun trackingId(trackingId: String): Builder

        /**
         * Creates an [AssetSubscriber] and starts subscribing to the asset location
         *
         * It is strongly suggested to call this method from the main thread.
         *
         * @return A new instance of [AssetSubscriber]
         * @throws BuilderConfigurationIncompleteException If all required params aren't set
         */
        fun start(): AssetSubscriber
    }
}
