package com.ably.tracking.subscriber.java

import com.ably.tracking.Resolution
import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.java.TrackableStateListener
import com.ably.tracking.subscriber.Subscriber
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.future

internal class DefaultSubscriberFacade(
    private val subscriber: Subscriber
) : SubscriberFacade, Subscriber by subscriber {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun resolutionPreferenceAsync(resolution: Resolution?): CompletableFuture<Void> {
        return scope.future { subscriber.resolutionPreference(resolution) }.thenRun { }
    }

    override fun addLocationListener(listener: LocationUpdateListener) {
        subscriber.locations
            .onEach { listener.onLocationUpdate(it) }
            .launchIn(scope)
    }

    override fun addRawLocationListener(listener: LocationUpdateListener) {
        subscriber.rawLocations
            .onEach { listener.onLocationUpdate(it) }
            .launchIn(scope)
    }

    override fun addListener(listener: TrackableStateListener) {
        subscriber.trackableStates
            .onEach { listener.onStateChanged(it) }
            .launchIn(scope)
    }

    override fun stopAsync(): CompletableFuture<Void> {
        return scope.future { subscriber.stop() }.thenRun { }
    }
}
