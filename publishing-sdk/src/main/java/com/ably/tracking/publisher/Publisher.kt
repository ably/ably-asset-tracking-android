package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission

typealias LocationUpdatedListener = (Location) -> Unit

/**
 * Represents a publisher, with associated [Trackable]. Publishers maintain the Ably connection, making use of
 * navigation resources as required to track [deliveries] as well as, [optionally][trackTransport], the transport
 * itself.
 */
interface Publisher {
    companion object Factory {
        /**
         * Returns the default builder of Publisher instances.
         */
        @JvmStatic
        fun publishers(): Builder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
            // TODO - keep a builder with default config and return it here instead of creating an empty new one
            return PublisherBuilder()
        }
    }

    /**
     * Whether this publisher should track its associated [Trackable], regardless of whether it has any [deliveries].
     *
     * The default state of this field can be set using [Builder.trackTransport].
     */
    var trackTransport: Boolean

    /**
     * Adds a delivery and makes it the actively tracked delivery, meaning that the state of the [trackedDelivery] field
     * will be updated to this delivery, if that wasn't already the case.
     *
     * If this delivery was already in this publisher's delivery set then this method only serves to change the actively
     * tracked delivery.
     */
    fun trackDelivery(delivery: Trackable)

    /**
     * Adds a delivery, but does not make it the actively tracked delivery, meaning that the state of the
     * [trackedDelivery] field will not change.
     *
     * If this delivery was already in this publisher's delivery set then this method does nothing.
     */
    fun addDelivery(delivery: Trackable)

    /**
     * Removes the delivery if it is known to this publisher, otherwise does nothing and returns false.
     *
     * If the removed delivery is the current [trackedDelivery] then that state will be cleared, meaning that for
     * another delivery to become the actively tracked delivery then the [trackDelivery] method must be subsequently
     * called.
     *
     * @return true if the delivery was known to this publisher.
     */
    fun removeDelivery(delivery: Trackable): Boolean

    /**
     * The actively tracked delivery, being the delivery whose destination is being used for navigation normalisation.
     *
     * This state can be changed by calling the [trackDelivery] method.
     */
    val trackedDelivery: Trackable?

    /**
     * Stops asset publisher from publishing asset location
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
        fun ably(configuration: AblyConfiguration): Builder

        /**
         * Sets the maps configuration.
         *
         * @param configuration Map library configuration object [MapConfiguration]
         * @return A new instance of the builder with map library configuration changed
         */
        fun map(configuration: MapConfiguration): Builder

        /**
         * Sets the logging configuration.
         *
         * @param configuration Logging configuration object [LogConfiguration]
         * @return A new instance of the builder with logging configuration changed
         */
        fun log(configuration: LogConfiguration): Builder

        /**
         * Sets listener that notifies about location updates
         *
         * This should probably be removed in the final version.
         *
         * @param listener Listener function that takes updated [Location] and returns nothing
         * @return A new instance of the builder with location updates listener changed
         */
        fun locationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets Android app context
         *
         * @param context App context
         * @return A new instance of the builder with android context changed
         */
        fun android(context: Context): Builder

        /**
         * Sets the transport to be associated for the lifespan of this publisher.
         *
         * Whether this transport is tracked from the outset of the publisher's existence can be controlled using the
         * [trackTransport] method.
         */
        fun transport(transport: Trackable): Builder

        /**
         * Sets the default state of [Publisher.trackTransport] field.
         *
         * @param track Whether the publisher should track its associated transport, regardless of whether it has any
         * deliveries, or a [Publisher.trackedDelivery].
         * @return A new instance of the builder with this property changed
         */
        fun trackTransport(track: Boolean): Builder

        /**
         * Creates a [Publisher] and starts publishing asset location
         *
         * In order to detect device's location ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission must be granted.
         * It is strongly suggested to call this method from the main thread.
         *
         * @return A new instance of [Publisher]
         * @throws BuilderConfigurationIncompleteException If all required params aren't set
         */
        @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
        fun start(): Publisher

        /**
         * Sets the debug configuration.
         *
         * @param configuration debug configuration object [DebugConfiguration]
         * @return A new instance of the builder with debug configuration changed
         */
        // TODO - probably should be removed in the final version
        // https://github.com/ably/ably-asset-tracking-android/issues/19
        fun debug(configuration: DebugConfiguration?): Builder
    }
}
