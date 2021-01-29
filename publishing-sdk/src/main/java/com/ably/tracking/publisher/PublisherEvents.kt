package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.PresenceMessage

internal sealed class Event

internal class StopEvent(
    val handler: ResultHandler<Unit>
) : Event()

internal class StartEvent : Event()

internal data class AddTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Event()

internal data class TrackTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Event()

internal data class SetActiveTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Event()

internal data class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    val handler: ResultHandler<Boolean>
) : Event()

internal data class JoinPresenceSuccessEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Event()

internal data class RawLocationChangedEvent(
    val locationUpdate: LocationUpdate
) : Event()

internal data class EnhancedLocationChangedEvent(
    val locationUpdate: EnhancedLocationUpdate
) : Event()

internal class RefreshResolutionPolicyEvent : Event()

internal data class SetDestinationSuccessEvent(
    val routeDurationInMilliseconds: Long
) : Event()

internal data class PresenceMessageEvent(
    val trackable: Trackable,
    val presenceMessage: PresenceMessage
) : Event()

internal class ChangeLocationEngineResolutionEvent : Event()

internal data class ChangeRoutingProfileEvent(
    val routingProfile: RoutingProfile
) : Event()
