package com.ably.tracking.publisher

import android.content.Context
import android.location.Location
import io.ably.lib.realtime.ConnectionStateListener

typealias LocationUpdatedListener = (Location) -> Unit

interface AssetPublisher {
    companion object Factory {
        /**
         * Returns the default builder of Publisher instances.
         */
        @JvmStatic
        fun publishers(): Builder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
            // TODO - keep a builder with default config and return it here instead of creating an empty new one
            return AssetPublisherBuilder()
        }
    }

    /**
     * Stops asset publisher from publishing asset location
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
         *  Sets the maps configuration.
         *
         * @param configuration Map library configuration object [MapConfiguration]
         * @return A new instance of the builder with map library configuration changed
         */
        fun mapConfig(configuration: MapConfiguration): Builder

        /**
         *  Sets the logging configuration.
         *
         * @param configuration Logging configuration object [LogConfiguration]
         * @return A new instance of the builder with logging configuration changed
         */
        fun logConfig(configuration: LogConfiguration): Builder

        /**
         *  Sets the battery configuration.
         *
         * @param configuration Battery configuration object [BatteryConfiguration]
         * @return A new instance of the builder with battery configuration changed
         */
        fun batteryConfig(configuration: BatteryConfiguration): Builder

        /**
         *  Sets the asset metadata
         *
         * @param metadataJsonString Asset metadata as a JSON string
         * @return A new instance of the builder with asset metadata changed
         */
        fun assetMetadataJson(metadataJsonString: String): Builder

        /**
         *  Sets the trip metadata
         *
         * @param metadataJsonString Trip metadata as a JSON string
         * @return A new instance of the builder with trip metadata changed
         */
        fun tripMetadataJson(metadataJsonString: String): Builder

        /**
         *  Sets listener that notifies about location updates
         *
         *  This should probably be removed in the final version.
         *
         * @param listener Listener function that takes updated [Location] and returns nothing
         * @return A new instance of the builder with location updates listener changed
         */
        fun locationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         *  Sets Android app context
         *
         * @param context App context
         * @return A new instance of the builder with android context changed
         */
        fun androidContext(context: Context): Builder

        /**
         *  Sets the travel destination and trackingId of the asset
         *
         *  @param trackingId Id of the tracked asset
         *  @param destination Travel destination, default empty string
         *  @param vehicleType Type of the vehicle, default "car"
         * @return A new instance of the builder with all above params updated
         */
        fun delivery(
            trackingId: String,
            destination: String = "",
            vehicleType: String = "car"
        ): Builder

        /**
         * Creates a [AssetPublisher] and starts publishing asset location
         *
         * @return A new instance of [AssetPublisher]
         * @throws BuilderConfigurationIncompleteException If all required params aren't set
         */
        fun start(): AssetPublisher

    }

}
