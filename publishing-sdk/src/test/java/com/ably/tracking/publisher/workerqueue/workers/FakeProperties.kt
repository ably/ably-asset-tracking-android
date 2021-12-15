package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Location
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.AddTrackableCallbackFunction
import com.ably.tracking.publisher.AddTrackableResult
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.EnhancedLocationChangedEvent
import com.ably.tracking.publisher.LocationsPublishingState
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherPropertiesDisposedException
import com.ably.tracking.publisher.RawLocationChangedEvent
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.SkippedLocations
import com.ably.tracking.publisher.Subscriber
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeProperties(
    override val duplicateTrackableGuard: DuplicateTrackableGuard,
    override val trackableRemovalGuard: TrackableRemovalGuard,
) : PublisherProperties {
    override val trackables: MutableSet<Trackable> = mutableSetOf()
    override val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>> = mutableMapOf()
    override var presenceData: PresenceData = PresenceData("properties")
    override val subscribers: MutableMap<String, MutableSet<Subscriber>> = mutableMapOf()
    override val requests: MutableMap<String, MutableMap<Subscriber, Resolution>> = mutableMapOf()
    override val resolutions: MutableMap<String, Resolution> = mutableMapOf()

    private var isDisposed: Boolean = false
    override var isStopped: Boolean = false
    override var locationEngineResolution: Resolution = Resolution(
        accuracy = Accuracy.BALANCED,
        desiredInterval = 299,
        minimumDisplacement = 2.4
    )
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var isTracking: Boolean = false
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val trackableStates: MutableMap<String, TrackableState> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val lastChannelConnectionStateChanges: MutableMap<String, ConnectionStateChange> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
        ConnectionState.OFFLINE, null
    )
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val lastSentRawLocations: MutableMap<String, Location> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val skippedEnhancedLocations: SkippedLocations = SkippedLocations()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val skippedRawLocations: SkippedLocations = SkippedLocations()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var estimatedArrivalTimeInMilliseconds: Long? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var lastPublisherLocation: Location? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var currentDestination: Destination? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override var active: Trackable? = null
    override var routingProfile: RoutingProfile = RoutingProfile.DRIVING
    override val rawLocationChangedCommands: MutableList<(DefaultCorePublisher.Properties) -> Unit> = mutableListOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationChangedEvent> =
        LocationsPublishingState()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    override val rawLocationsPublishingState: LocationsPublishingState<RawLocationChangedEvent> =
        LocationsPublishingState()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
}

internal class FakeDuplicateGuard(private val currentlyAdding: Boolean) : DuplicateTrackableGuard {
    private val trackables: MutableSet<Trackable> = mutableSetOf()

    override fun startAddingTrackable(trackable: Trackable) {
        trackables.add(trackable)
    }

    override fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>) {
        // does not need implementing for now
    }

    override fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean {
        return currentlyAdding
    }

    override fun saveDuplicateAddHandler(trackable: Trackable, callbackFunction: AddTrackableCallbackFunction) {
        // does not need implementing
    }

    override fun clear(trackable: Trackable) {
        trackables.remove(trackable)
    }

    override fun clearAll() {
        trackables.clear()
    }
}
