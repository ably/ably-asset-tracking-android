package com.ably.tracking.publisher

import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.ResultHandler
import com.ably.tracking.TrackableState
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
internal sealed class Request<T>(val handler: ResultHandler<T>) : Event()

internal class StopEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal class AddTrackableEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

internal class TrackTrackableEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

internal class SetActiveTrackableEvent(
    val trackable: Trackable,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    handler: ResultHandler<Boolean>
) : Request<Boolean>(handler)

internal class DisconnectSuccessEvent(
    val trackable: Trackable,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

internal class ConnectionForTrackableCreatedEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

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

internal data class AblyConnectionStateChangeEvent(val connectionStateChange: ConnectionStateChange) : AdhocEvent()

internal data class ChannelConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange,
    val trackableId: String
) : AdhocEvent()
