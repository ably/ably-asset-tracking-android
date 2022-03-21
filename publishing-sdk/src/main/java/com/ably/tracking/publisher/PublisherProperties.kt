package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This interface is intended to make it easy to create fake properties for tests
 * */
internal interface PublisherProperties {
    val duplicateTrackableGuard: DuplicateTrackableGuard
    val trackableRemovalGuard: TrackableRemovalGuard
    val trackables: MutableSet<Trackable>
    val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>>
    var presenceData: PresenceData
    val subscribers: MutableMap<String, MutableSet<Subscriber>>
    val requests: MutableMap<String, MutableMap<Subscriber, Resolution>>
    val resolutions: MutableMap<String, Resolution>
    var active: Trackable?
    var isStopped: Boolean
    var locationEngineResolution: Resolution
    val isLocationEngineResolutionConstant: Boolean
    var isTracking: Boolean
    val trackableStates: MutableMap<String, TrackableState>
    val lastChannelConnectionStateChanges: MutableMap<String, ConnectionStateChange>
    var lastConnectionStateChange: ConnectionStateChange
    val lastSentEnhancedLocations: MutableMap<String, Location>
    val lastSentRawLocations: MutableMap<String, Location>
    val skippedEnhancedLocations: SkippedLocations
    val skippedRawLocations: SkippedLocations
    var estimatedArrivalTimeInMilliseconds: Long?
    var lastPublisherLocation: Location?
    var currentDestination: Destination?
    var routingProfile: RoutingProfile
    val rawLocationChangedCommands: MutableList<(PublisherProperties) -> Unit>
    val enhancedLocationsPublishingState: LocationsPublishingState<EnhancedLocationUpdate>
    val rawLocationsPublishingState: LocationsPublishingState<RawLocationChangedEvent>
    val areRawLocationsEnabled: Boolean
    fun dispose()
}
