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
 * Represents an event that invokes an action that calls the [handler] when it completes.
 */
sealed class Request<T>(val callbackFunction: ResultCallbackFunction<T>) : Event()

/**
 * Stop the [CorePublisher].
 */
internal class StopEvent(
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Add a [Trackable] to the [CorePublisher].
 */
internal class AddTrackableEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Failed to add a [Trackable].
 * Should be created only from within the [CorePublisher].
 */
internal class AddTrackableFailedEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler,
    val exception: Exception,
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Track a [Trackable].
 */
internal class TrackTrackableEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

/**
 * Change the actively tracked [Trackable].
 * Should be created only from within the [CorePublisher].
 */
internal class SetActiveTrackableEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Remove a [Trackable] from the [CorePublisher].
 */
internal class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    callbackFunction: ResultCallbackFunction<Boolean>
) : Request<Boolean>(callbackFunction)

/**
 * Successfully disconnected from the trackable channel.
 * Should be created only from within the [CorePublisher].
 */
internal class DisconnectSuccessEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<Unit>
) : Request<Unit>(callbackFunction)

/**
 * Successfully created a connection for a trackable channel.
 * Should be created only from within the [CorePublisher].
 */
internal class ConnectionForTrackableCreatedEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

/**
 * Requested removal of a trackable that is during add process.
 * Should be created only from within the [CorePublisher].
 */
internal class TrackableRemovalRequestedEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    val result: Result<Unit>
) : Request<StateFlow<TrackableState>>(callbackFunction)

/**
 * Connection for a trackable is ready to be used.
 * Should be created only from within the [CorePublisher].
 */
internal class ConnectionForTrackableReadyEvent(
    val trackable: Trackable,
    callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(callbackFunction)

/**
 * A new raw location update is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class RawLocationChangedEvent(
    val location: Location,
) : AdhocEvent()

/**
 * Successfully sent a raw location update.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendRawLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

/**
 * Failed to send a raw location update.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendRawLocationFailureEvent(
    val locationUpdate: LocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
) : AdhocEvent()

/**
 * A new enhanced location update is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class EnhancedLocationChangedEvent(
    val location: Location,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : AdhocEvent()

/**
 * Successfully sent an enhanced location update.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendEnhancedLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

/**
 * Failed to send an enhanced location update.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendEnhancedLocationFailureEvent(
    val locationUpdate: EnhancedLocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
) : AdhocEvent()

/**
 * Refresh the resolution policy.
 * Should be created only from within the [CorePublisher].
 */
internal class RefreshResolutionPolicyEvent : AdhocEvent()

/**
 * Successfully set the destination.
 * Should be created only from within the [CorePublisher].
 */
internal data class SetDestinationSuccessEvent(
    val routeDurationInMilliseconds: Long
) : AdhocEvent()

/**
 * A new presence message is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class PresenceMessageEvent(
    val trackable: Trackable,
    val presenceMessage: PresenceMessage
) : AdhocEvent()

/**
 * Change the location engine resolution.
 * Should be created only from within the [CorePublisher].
 */
internal class ChangeLocationEngineResolutionEvent : AdhocEvent()

/**
 * Change the navigation routing profile.
 */
internal data class ChangeRoutingProfileEvent(
    val routingProfile: RoutingProfile
) : AdhocEvent()

/**
 * Ably connection state changed.
 * Should be created only from within the [CorePublisher].
 */
internal data class AblyConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()

/**
 * Trackable Ably channel state changed.
 * Should be created only from within the [CorePublisher].
 */
internal data class ChannelConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange,
    val trackableId: String
) : AdhocEvent()
