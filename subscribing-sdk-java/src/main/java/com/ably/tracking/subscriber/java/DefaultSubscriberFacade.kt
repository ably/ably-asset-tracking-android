package com.ably.tracking.subscriber.java

import com.ably.tracking.Resolution
import com.ably.tracking.java.AssetStatusListener
import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.subscriber.Subscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

internal class DefaultSubscriberFacade(
    private val subscriber: Subscriber
) : SubscriberFacade {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun sendChangeRequestAsync(resolution: Resolution): CompletableFuture<Void> {
        return scope.future { subscriber.sendChangeRequest(resolution) }.thenRun { }
    }

    override fun addLocationListener(listener: LocationUpdateListener) {
        subscriber.locations
            .onEach { listener.onLocationUpdate(it) }
            .launchIn(scope)
    }

    override fun addListener(listener: AssetStatusListener) {
        subscriber.assetStatuses
            .onEach { listener.onStatusChanged(it) }
            .launchIn(scope)
    }

    override fun stopAsync(): CompletableFuture<Void> {
        return scope.future { subscriber.stop() }.thenRun { }
    }
}
