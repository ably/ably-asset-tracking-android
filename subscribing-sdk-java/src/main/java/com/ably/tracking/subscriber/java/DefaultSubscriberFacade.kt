package com.ably.tracking.subscriber.java

import com.ably.tracking.java.AssetStatusListener
import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.Resolution
import com.ably.tracking.subscriber.Subscriber
import java.util.concurrent.CompletableFuture

internal class DefaultSubscriberFacade(
    subscriber: Subscriber
) : SubscriberFacade {
    override fun sendChangeRequestAsync(resolution: Resolution): CompletableFuture<Void> {
        TODO()
    }

    override fun addEnhancedLocationListener(listener: LocationUpdateListener) {
        TODO()
    }

    override fun addListener(listener: AssetStatusListener) {
        TODO()
    }

    override fun stopAsync(): CompletableFuture<Void> {
        TODO()
    }
}
