package com.ably.tracking.subscriber.java

import com.ably.tracking.Resolution
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.java.LocationUpdateIntervalListener
import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.java.PublisherPresenceListener
import com.ably.tracking.java.ResolutionListener
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

    @Suppress("DEPRECATION")
    @Deprecated("Use sendResolutionPreferenceAsync instead")
    override fun resolutionPreferenceAsync(resolution: Resolution?): CompletableFuture<Void> {
        return scope.future { subscriber.resolutionPreference(resolution) }.thenRun { }
    }

    override fun sendResolutionPreferenceAsync(resolution: Resolution?) =
        subscriber.sendResolutionPreference(resolution)

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

    @Experimental
    override fun addPublisherPresenceListener(listener: PublisherPresenceListener) {
        subscriber.publisherPresence
            .onEach { listener.onPublisherPresenceChanged(it) }
            .launchIn(scope)
    }

    override fun addResolutionListener(listener: ResolutionListener) {
        subscriber.resolutions
            .onEach { listener.onResolutionChanged(it) }
            .launchIn(scope)
    }

    override fun addNextLocationUpdateIntervalListener(listener: LocationUpdateIntervalListener) {
        subscriber.nextLocationUpdateIntervals
            .onEach { listener.onLocationUpdateIntervalChanged(it) }
            .launchIn(scope)
    }

    override fun stopAsync(): CompletableFuture<Void> {
        return scope.future { subscriber.stop() }.thenRun { }
    }
}
