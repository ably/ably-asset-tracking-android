package com.ably.tracking.publisher.java

import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.Trackable
import java.util.concurrent.CompletableFuture

/**
 * Methods provided for those using the [Publisher] from Java code (Java 1.8 or higher).
 *
 * Kotlin users will generally prefer to directly use the interfaces offered by [Publisher].
 */
interface PublisherFacade {
    /**
     * Adds a [Trackable] object and makes it the actively tracked object, meaning that the state of the [active] field
     * will be updated to this object, if that wasn't already the case.
     *
     * If this object was already in this publisher's tracked set then this method only serves to change the actively
     * tracked object, and then completing successfully.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there, and to be
     * made the actively tracked object.
     *
     * @return A [CompletableFuture] that completes when the object has been removed.
     */
    fun trackAsync(trackable: Trackable): CompletableFuture<Void>

    /**
     * Adds a [Trackable] object, but does not make it the actively tracked object, meaning that the state of the
     * [active] field will not change.
     *
     * If this object was already in this publisher's tracked set then this method does nothing,
     * simply completing successfully.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there.
     *
     * @return A [CompletableFuture] that completes when the object has been added.
     */
    fun addAsync(trackable: Trackable): CompletableFuture<Void>

    /**
     * Removes a [Trackable] object if it is known to this publisher, otherwise does nothing and returns false.
     *
     * If the removed object is the current actively [active] object then that state will be cleared, meaning that for
     * another object to become the actively tracked delivery then the [track] method must be subsequently called.
     *
     * @param trackable The object to be removed from this publisher's tracked set, it it's there.
     *
     * @return A [CompletableFuture] that completes when the object has been removed.
     */
    fun removeAsync(trackable: Trackable): CompletableFuture<Boolean>

    /**
     * Add a listener to receive location values when they become available.
     */
    fun addListener(listener: LocationUpdateListener)

    /**
     * Stops this publisher from publishing locations. Once a publisher has been stopped, it cannot be restarted.
     *
     * This method overload is provided for the convenient of those calling from Java.
     * Kotlin users will generally prefer to use the [stop] method.
     *
     * @return A [CompletableFuture] that completes when the Publisher has been stopped.
     */
    fun stopAsync(): CompletableFuture<Void>

    companion object {
        /**
         * Returns a facade for the given publisher instance.
         */
        @JvmStatic
        fun wrap(publisher: Publisher): PublisherFacade {
            return DefaultPublisherFacade(publisher)
        }
    }
}
