package com.ably.tracking.publisher.java

import com.ably.tracking.java.LocationUpdateListener
import com.ably.tracking.java.TrackableStateListener
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class DefaultPublisherFacade(
    private val publisher: Publisher
) : PublisherFacade, Publisher by publisher {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun trackAsync(trackable: Trackable, listener: TrackableStateListener?): CompletableFuture<Void> {
        return scope.future {
            val flow = publisher.track(trackable)
            listener?.let { trackableStateListener ->
                flow.onEach { trackableState -> trackableStateListener.onStateChanged(trackableState) }
                    .launchIn(scope)
            }
        }.thenRun { }
    }

    override fun addAsync(trackable: Trackable, listener: TrackableStateListener?): CompletableFuture<Void> {
        return scope.future {
            val flow = publisher.add(trackable)
            listener?.let { trackableStateListener ->
                flow.onEach { trackableState -> trackableStateListener.onStateChanged(trackableState) }
                    .launchIn(scope)
            }
        }.thenRun { }
    }

    override fun removeAsync(trackable: Trackable): CompletableFuture<Boolean> {
        return scope.future { publisher.remove(trackable) }
    }

    override fun addListener(listener: LocationUpdateListener) {
        publisher.locations
            .onEach { listener.onLocationUpdate(it) }
            .launchIn(scope)
    }

    override fun addTrackablesListener(listener: TrackablesListener) {
        publisher.trackables
            .onEach { listener.onTrackables(it) }
            .launchIn(scope)
    }

    override fun addLocationHistoryListener(listener: LocationHistoryListener) {
        publisher.locationHistory
            .onEach { listener.onLocationHistory(it) }
            .launchIn(scope)
    }

    override fun addTrackableStateListener(trackableId: String, listener: TrackableStateListener) {
        publisher.getTrackableState(trackableId)
            ?.onEach { listener.onStateChanged(it) }
            ?.launchIn(scope)
    }

    override fun stopAsync(): CompletableFuture<Void> {
        return scope.future { publisher.stop() }.thenRun { }
    }
}
