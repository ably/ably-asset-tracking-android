package com.ably.tracking.publisher.java

import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.Trackable
import java.util.concurrent.CompletableFuture

class DefaultPublisherFacade(
    publisher: Publisher
) : PublisherFacade {
    init {
        publisher.javaClass // fix for lint's complaint that [publisher] is never used - should be removed
    }
    override fun trackAsync(trackable: Trackable): CompletableFuture<Void> {
        TODO()
    }

    override fun addAsync(trackable: Trackable): CompletableFuture<Void> {
        TODO()
    }

    override fun removeAsync(trackable: Trackable): CompletableFuture<Boolean> {
        TODO()
    }

    override fun addListener(listener: LocationUpdateListener) {
        TODO()
    }

    override fun addLocationHistoryListener(listener: LocationHistoryListener) {
        TODO()
    }

    override fun stopAsync(): CompletableFuture<Void> {
        TODO()
    }
}
