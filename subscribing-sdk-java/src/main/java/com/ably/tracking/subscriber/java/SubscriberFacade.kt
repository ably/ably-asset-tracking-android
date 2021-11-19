package com.ably.tracking.subscriber.java

import com.ably.tracking.Resolution
import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.java.TrackableStateListener
import com.ably.tracking.subscriber.Subscriber
import java.util.concurrent.CompletableFuture

/**
 * Methods provided for those using the [Subscriber] from Java code (Java 1.8 or higher).
 *
 * Kotlin users will generally prefer to directly use the interfaces offered by [Subscriber].
 */
interface SubscriberFacade : Subscriber {
    /**
     * Sends the preferred resolution for updates, to be requested from the remote publisher.
     *
     * An initial resolution may be defined from the outset of a [Subscriber]'s lifespan by using the
     * [resolution][Builder.resolution] method on the [Builder] instance used to [start][Builder.start] it.
     *
     * Requests sent using this method will take time to propagate back to the publisher.
     *
     * The returned [CompletableFuture] will complete once the request has been successfully registered with the server,
     * however this does not necessarily mean that the request has been received and actioned by the publisher.
     *
     * @param resolution The preferred resolution or null if has no resolution preference.
     *
     * @return A [CompletableFuture] that completes when the request has been completed.
     */
    fun resolutionPreferenceAsync(resolution: Resolution?): CompletableFuture<Void>

    /**
     * Adds a handler to be notified when an enhanced location update is available.
     *
     * @param listener The listening function to be notified.
     */
    fun addLocationListener(listener: LocationUpdateListener)

    /**
     * Adds a handler to be notified when a raw location update is available.
     *
     * @param listener The listening function to be notified.
     */
    fun addRawLocationListener(listener: LocationUpdateListener)

    /**
     * Adds a handler to be notified when the online state of the trackable changes.
     *
     * @param listener the listening function to be notified.
     */
    fun addListener(listener: TrackableStateListener)

    /**
     * Stops this subscriber from listening to published locations. Once a subscriber has been stopped, it cannot be
     * restarted.
     *
     * @return A [CompletableFuture] that completes when the object has been removed.
     */
    fun stopAsync(): CompletableFuture<Void>

    /**
     * Builder for providing [SubscriberFacade].
     */
    interface Builder {
        companion object {
            /**
             * Returns a facade for the given subscriber builder instance.
             */
            @JvmStatic
            fun wrap(builder: Subscriber.Builder): Builder {
                return SubscriberFacadeBuilder(builder)
            }
        }

        /**
         * Creates a [SubscriberFacade] and starts listening for location updates.
         *
         * @return A [CompletableFuture] with the created and started subscriber facade.
         */
        fun startAsync(): CompletableFuture<SubscriberFacade>
    }
}
