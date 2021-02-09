package com.ably.tracking.publisher

import com.ably.tracking.AssetStatus
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.PresenceMessage
import kotlinx.coroutines.flow.StateFlow

internal sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
internal sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls a callback when it completes.
 */
internal sealed class Request : Event()

internal class StopEvent(
    val handler: ResultHandler<Unit>
) : Request()

internal class StartEvent : AdhocEvent()

internal data class AddTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<StateFlow<AssetStatus>>
) : Request()

internal data class TrackTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<StateFlow<AssetStatus>>
) : Request()

internal data class SetActiveTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Request()

internal data class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    val handler: ResultHandler<Boolean>
) : Request()

internal data class JoinPresenceSuccessEvent(
    val trackable: Trackable,
    val handler: ResultHandler<StateFlow<AssetStatus>>
) : Request()

internal data class RawLocationChangedEvent(
    val locationUpdate: LocationUpdate
) : AdhocEvent()

internal data class EnhancedLocationChangedEvent(
    val locationUpdate: EnhancedLocationUpdate
) : AdhocEvent()

internal class RefreshResolutionPolicyEvent : AdhocEvent()

internal data class SetDestinationSuccessEvent(
    val routeDurationInMilliseconds: Long
) : AdhocEvent()

internal data class PresenceMessageEvent(
    val trackable: Trackable,
    val presenceMessage: PresenceMessage
) : AdhocEvent()

internal class ChangeLocationEngineResolutionEvent : AdhocEvent()

internal data class ChangeRoutingProfileEvent(
    val routingProfile: RoutingProfile
) : AdhocEvent()
