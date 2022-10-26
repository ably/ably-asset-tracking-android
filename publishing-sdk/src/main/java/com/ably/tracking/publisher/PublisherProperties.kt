package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.guards.DefaultDuplicateTrackableGuard
import com.ably.tracking.publisher.guards.DefaultTrackableRemovalGuard
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import kotlinx.coroutines.flow.MutableStateFlow

internal class PublisherProperties(
    routingProfile: RoutingProfile,
    locationEngineResolution: Resolution,
    isLocationEngineResolutionConstant: Boolean,
    areRawLocationsEnabled: Boolean?,
    private val onActiveTrackableUpdated: (Trackable?) -> Unit,
    private val onRoutingProfileUpdated: (RoutingProfile) -> Unit
) {
    private var isDisposed: Boolean = false
    var locationEngineResolution: Resolution = locationEngineResolution
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val isLocationEngineResolutionConstant: Boolean = isLocationEngineResolutionConstant
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var isTracking: Boolean = false
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val trackables: MutableSet<Trackable> = mutableSetOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val trackableStates: MutableMap<String, TrackableState> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val trackableSubscribedToPresenceFlags: MutableMap<String, Boolean> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val lastChannelConnectionStateChanges: MutableMap<String, ConnectionStateChange> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
        ConnectionState.OFFLINE, null
    )
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val resolutions: MutableMap<String, Resolution> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val lastSentRawLocations: MutableMap<String, Location> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val skippedEnhancedLocations: SkippedLocations = SkippedLocations()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val skippedRawLocations: SkippedLocations = SkippedLocations()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var estimatedArrivalTimeInMilliseconds: Long? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var lastPublisherLocation: Location? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var currentDestination: Destination? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val subscribers: MutableMap<String, MutableSet<Subscriber>> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val requests: MutableMap<String, MutableMap<Subscriber, Resolution>> = mutableMapOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var presenceData: PresenceData =
        PresenceData(ClientTypes.PUBLISHER, rawLocations = areRawLocationsEnabled)
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    var active: Trackable? = null
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        set(value) {
            onActiveTrackableUpdated(value)
            field = value
        }
    var routingProfile: RoutingProfile = routingProfile
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
        set(value) {
            onRoutingProfileUpdated(routingProfile)
            field = value
        }
    val rawLocationChangedCommands: MutableList<(PublisherProperties) -> Unit> = mutableListOf()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationUpdate> =
        LocationsPublishingState()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val rawLocationsPublishingState: LocationsPublishingState<LocationUpdate> =
        LocationsPublishingState()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val duplicateTrackableGuard: DuplicateTrackableGuard = DefaultDuplicateTrackableGuard()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val trackableRemovalGuard: TrackableRemovalGuard = DefaultTrackableRemovalGuard()
        get() = if (isDisposed) throw PublisherPropertiesDisposedException() else field
    val areRawLocationsEnabled: Boolean = areRawLocationsEnabled ?: false
    var state: PublisherState = PublisherState.IDLE
        set(value) {
            // Once we stop publisher it should never change its state
            if (field == PublisherState.STOPPED) {
                throw PublisherStoppedException()
            }
            field = value
        }
    val hasNoTrackablesAddingOrAdded: Boolean
        get() = trackables.isEmpty() && !duplicateTrackableGuard.isCurrentlyAddingAnyTrackable()

    fun dispose() {
        trackables.clear()
        trackableStates.clear()
        trackableStateFlows.clear()
        trackableSubscribedToPresenceFlags.clear()
        lastChannelConnectionStateChanges.clear()
        resolutions.clear()
        lastSentEnhancedLocations.clear()
        lastSentRawLocations.clear()
        skippedEnhancedLocations.clearAll()
        skippedRawLocations.clearAll()
        estimatedArrivalTimeInMilliseconds = null
        active = null
        lastPublisherLocation = null
        currentDestination = null
        subscribers.clear()
        requests.clear()
        rawLocationChangedCommands.clear()
        enhancedLocationsPublishingState.clearAll()
        rawLocationsPublishingState.clearAll()
        duplicateTrackableGuard.clearAll()
        trackableRemovalGuard.clearAll()
        isDisposed = true
    }
}
