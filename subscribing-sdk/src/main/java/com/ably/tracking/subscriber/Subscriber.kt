package com.ably.tracking.subscriber

import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionException
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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
            return SubscriberBuilder()
        }
    }

    /**
     * Sends the preferred resolution for updates, to be requested from the remote publisher.
     *
     * An initial resolution may be defined from the outset of a [Subscriber]'s lifespan by using the
     * [resolution][Builder.resolution] method on the [Builder] instance used to [start][Builder.start] it.
     *
     * Requests sent using this method will take time to propagate back to the publisher.
     *
     * The [handler] will be called once the request has been successfully registered with the server,
     * however this does not necessarily mean that the request has been received and actioned by the publisher.
     *
     * @param resolution The preferred resolution or null if has no resolution preference.
     */
    @JvmSynthetic
    suspend fun resolutionPreference(resolution: Resolution?)

    /**
     * The shared flow emitting enhanced location values when they become available.
     */
    val locations: SharedFlow<LocationUpdate>
        @JvmSynthetic get

    /**
     * The shared flow emitting raw location values when they become available.
     * Raw locations are disabled by default. You need to enable them in the Publishing SDK.
     */
    val rawLocations: SharedFlow<LocationUpdate>
        @JvmSynthetic get

    /**
     * The shared flow emitting values when the state of the trackable changes.
     */
    val trackableStates: StateFlow<TrackableState>
        @JvmSynthetic get

    /**
     * The shared flow emitting resolution values when they become available.
     */
    val resolutions: SharedFlow<Resolution>
        @JvmSynthetic get

    /**
     * The shared flow emitting the estimated next location update intervals in milliseconds when they become available.
     */
    val nextLocationUpdateIntervals: SharedFlow<Long>
        @JvmSynthetic get

    /**
     * Stops this subscriber from listening to published locations. Once a subscriber has been stopped, it cannot be
     * restarted.
     *
     * @throws ConnectionException If something goes wrong during connection closing
     */
    @JvmSynthetic
    suspend fun stop()

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
         * **REQUIRED** Sets the Ably connection configuration.
         *
         * @param configuration The configuration to be used for Ably connection.
         * @return A new instance of the builder with this property changed.
         */
        fun connection(configuration: ConnectionConfiguration): Builder

        /**
         * **OPTIONAL** Sets the preferred resolution of updates, to be requested from the remote publisher.
         *
         * @param resolution An indication of how often to this subscriber would like the publisher to sample locations,
         * at what level of positional accuracy, and how often to send them back.
         * @return A new instance of the builder with this property changed.
         */
        fun resolution(resolution: Resolution): Builder

        /**
         * **REQUIRED** Sets the asset to be tracked, using its unique tracking identifier.
         *
         * @param trackingId The unique tracking identifier for the asset.
         * @return A new instance of the builder with this property changed.
         */
        fun trackingId(trackingId: String): Builder

        /**
         * EXPERIMENTAL API
         * **OPTIONAL** Sets the log handler.
         *
         * @param logHandler The class that will handle log messages.
         * @return A new instance of the builder with this property changed.
         */
        fun logHandler(logHandler: LogHandler): Builder

        /**
         * Creates a [Subscriber] and starts listening for location updates.
         *
         * @return A new subscriber instance.
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         * @throws ConnectionException If something goes wrong during connection initialization
         */
        @JvmSynthetic
        @Throws(BuilderConfigurationIncompleteException::class, ConnectionException::class)
        suspend fun start(): Subscriber
    }
}
