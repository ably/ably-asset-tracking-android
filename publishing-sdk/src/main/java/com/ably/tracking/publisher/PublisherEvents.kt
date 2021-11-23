package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import kotlinx.coroutines.flow.StateFlow

internal typealias AddTrackableResult = StateFlow<TrackableState>
internal typealias AddTrackableHandler = ResultCallbackFunction<AddTrackableResult>

sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls a callback when it completes.
 */
sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()

internal class StopEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class AddTrackableEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler
) : Request<StateFlow<TrackableState>>(handler)

internal class AddTrackableFailedEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler,
    val exception: Exception,
) : Request<StateFlow<TrackableState>>(handler)

internal class TrackTrackableEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

internal class SetActiveTrackableEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    callbackFunction: ResultCallbackFunction<Boolean>
) : Request<Boolean>(callbackFunction)

internal class DisconnectSuccessEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

internal class ConnectionForTrackableCreatedEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

internal class TrackableRemovalRequestedEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    val result: Result<Unit>
) : Request<StateFlow<TrackableState>>(callbackFunction)

internal class ConnectionForTrackableReadyEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

data class RawLocationChangedEvent(
    val location: Location,
) : AdhocEvent()

internal data class SendRawLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

internal data class SendRawLocationFailureEvent(
    val locationUpdate: LocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
) : AdhocEvent()

data class EnhancedLocationChangedEvent(
    val location: Location,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : AdhocEvent()

internal data class SendEnhancedLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

internal data class SendEnhancedLocationFailureEvent(
    val locationUpdate: EnhancedLocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
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
