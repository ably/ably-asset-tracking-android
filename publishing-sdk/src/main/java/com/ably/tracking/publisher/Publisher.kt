package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LogConfiguration

typealias LocationUpdatedListener = (Location) -> Unit

/**
 * Represents a publisher. Publishers maintain the Ably connection, making use of navigation resources as required to
 * track [Trackable] objects.
 */
interface Publisher {
    companion object Factory {
        /**
         * Returns the default state of the publisher [Builder], which is incapable of starting of [Publisher]
         * instances until it has been configured fully.
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
     * Adds a [Trackable] object and makes it the actively tracked object, meaning that the state of the [active] field
     * will be updated to this object, if that wasn't already the case.
     *
     * If this object was already in this publisher's tracked set then this method only serves to change the actively
     * tracked object.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there, and to be
     * made the actively tracked object.
     */
    fun track(trackable: Trackable)

    /**
     * Adds a [Trackable] object, but does not make it the actively tracked object, meaning that the state of the
     * [active] field will not change.
     *
     * If this object was already in this publisher's tracked set then this method does nothing.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there.
     */
    fun add(trackable: Trackable)

    /**
     * Removes a [Trackable] object if it is known to this publisher, otherwise does nothing and returns false.
     *
     * If the removed object is the current actively [active] object then that state will be cleared, meaning that for
     * another object to become the actively tracked delivery then the [track] method must be subsequently called.
     *
     * @param trackable The object to be removed from this publisher's tracked set, it it's there.
     * @return true if the object was known to this publisher, being that it was in the tracked set.
     */
    fun remove(trackable: Trackable): Boolean

    /**
     * The actively tracked object, being the [Trackable] object whose destination will be used for location
     * enhancement, if available.
     *
     * This state can be changed by calling the [track] method.
     */
    val active: Trackable?

    /**
     * The active means of transport for this publisher.
     */
    var transportationMode: TransportationMode

    /**
     * Stops this publisher from publishing locations. Once a publisher has been stopped, it cannot be restarted.
     *
     * It is strongly suggested to call this method from the main thread.
     */
    fun stop()

    /**
     * The methods implemented by builders capable of starting [Publisher] instances.
     *
     * All methods except [start] return a new [Builder] instance, being a copy of this instance but with the
     * relevant property mutated.
     *
     * The starting point is always the default builder state, returned by the static [publishers] method.
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
         * Sets the maps configuration.
         *
         * @param configuration The configuration to be used for maps.
         * @return A new instance of the builder with this property changed.
         */
        fun map(configuration: MapConfiguration): Builder

        /**
         * Sets the logging configuration.
         *
         * @param configuration The configuration to be used for logging.
         * @return A new instance of the builder with this property changed.
         */
        fun log(configuration: LogConfiguration): Builder

        /**
         * Sets a listener to be notified about location updates.
         *
         * @param listener The function, which will be called once per [Location] update.
         * @return A new instance of the builder with this property changed.
         */
        fun locationUpdatedListener(listener: LocationUpdatedListener): Builder

        /**
         * Sets the Android Context.
         *
         * @param context The context of the application.
         * @return A new instance of the builder with this property changed.
         */
        fun androidContext(context: Context): Builder

        /**
         * Set the means of transport being used for the initial state of publishers created from this builder.
         *
         * @param mode The means of transport.
         * @return A new instance of the builder with this property changed.
         */
        fun mode(mode: TransportationMode): Builder

        /**
         * Sets the policy factory to be used to define the target resolution for publishers created from this builder.
         *
         * @param factory The factory, whose [createResolutionPolicy][ResolutionPolicy.Factory.createResolutionPolicy]
         * method will be called exactly once when [start] is called.
         * @return A new instance of the builder with this property changed.
         */
        fun resolutionPolicy(factory: ResolutionPolicy.Factory): Builder

        /**
         * Creates a [Publisher] and starts publishing.
         *
         * The returned publisher instance does not start in a state whereby it is actively tracking anything. If
         * tracking is required from the outset then the [track][Publisher.track] method must be subsequently called.
         *
         * In order to detect device's location ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission must be granted.
         * It is strongly suggested to call this method from the main thread.
         *
         * @return A new publisher instance.
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         */
        @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
        fun start(): Publisher

        /**
         * Sets the debug configuration.
         *
         * @param configuration debug configuration object [DebugConfiguration]
         * @return A new instance of the builder with this property changed.
         */
        // TODO - probably should be removed in the final version
        // https://github.com/ably/ably-asset-tracking-android/issues/19
        fun debug(configuration: DebugConfiguration?): Builder
    }
}
