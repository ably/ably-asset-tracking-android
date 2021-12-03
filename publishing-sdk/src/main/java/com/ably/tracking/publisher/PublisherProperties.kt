package com.ably.tracking.publisher

import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
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
}
