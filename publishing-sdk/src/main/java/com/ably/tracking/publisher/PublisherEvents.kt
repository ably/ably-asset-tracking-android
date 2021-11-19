package com.ably.tracking.publisher

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultHandler
import kotlinx.coroutines.flow.StateFlow

internal typealias AddTrackableResult = StateFlow<TrackableState>
internal typealias AddTrackableHandler = ResultHandler<AddTrackableResult>

internal sealed class Event

/**
 * Represents an event that doesn't have a callback (launch and forget).
 */
internal sealed class AdhocEvent : Event()

/**
 * Represents an event that invokes an action that calls the [handler] when it completes.
 */
internal sealed class Request<T>(val handler: ResultHandler<T>) : Event()

/**
 * Event created when wanting to stop the [CorePublisher].
 */
internal class StopEvent(
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when wanting to add a [Trackable] to the [CorePublisher].
 */
internal class AddTrackableEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Event created when adding a [Trackable] fails.
 * Should be created only from within the [CorePublisher].
 */
internal class AddTrackableFailedEvent(
    val trackable: Trackable,
    handler: AddTrackableHandler,
    val exception: Exception,
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Event created when wanting to track a [Trackable].
 */
internal class TrackTrackableEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Event created when wanting to change the actively tracked [Trackable].
 * Should be created only from within the [CorePublisher].
 */
internal class SetActiveTrackableEvent(
    val trackable: Trackable,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when wanting to remove a [Trackable] from the [CorePublisher].
 */
internal class RemoveTrackableEvent(
    val trackable: Trackable,

    /**
     * On success, the handler is supplied `true` if the [Trackable] was already present.
     */
    handler: ResultHandler<Boolean>
) : Request<Boolean>(handler)

/**
 * Event created when successfully disconnected from the trackable channel.
 * Should be created only from within the [CorePublisher].
 */
internal class DisconnectSuccessEvent(
    val trackable: Trackable,
    handler: ResultHandler<Unit>
) : Request<Unit>(handler)

/**
 * Event created when a connection for a trackable is successfully created.
 * Should be created only from within the [CorePublisher].
 */
internal class ConnectionForTrackableCreatedEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Event created when a connection for a trackable is ready to be used.
 * Should be created only from within the [CorePublisher].
 */
internal class ConnectionForTrackableReadyEvent(
    val trackable: Trackable,
    handler: ResultHandler<StateFlow<TrackableState>>
) : Request<StateFlow<TrackableState>>(handler)

/**
 * Event created each time a new raw location update is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class RawLocationChangedEvent(
    val location: Location,
) : AdhocEvent()

/**
 * Event created when sending a raw location was successful.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendRawLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

/**
 * Event created when sending a raw location failed.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendRawLocationFailureEvent(
    val locationUpdate: LocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
) : AdhocEvent()

/**
 * Event created each time a new enhanced location update is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class EnhancedLocationChangedEvent(
    val location: Location,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : AdhocEvent()

/**
 * Event created when sending an enhanced location was successful.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendEnhancedLocationSuccessEvent(
    val location: Location,
    val trackableId: String,
) : AdhocEvent()

/**
 * Event created when sending an enhanced location failed.
 * Should be created only from within the [CorePublisher].
 */
internal data class SendEnhancedLocationFailureEvent(
    val locationUpdate: EnhancedLocationUpdate,
    val trackableId: String,
    val exception: Throwable?,
) : AdhocEvent()

/**
 * Event created when the resolution policy should be refreshed. This means recalculating all the resolutions.
 * Should be created only from within the [CorePublisher].
 */
internal class RefreshResolutionPolicyEvent : AdhocEvent()

/**
 * Event created when setting the destination was successful.
 * Should be created only from within the [CorePublisher].
 */
internal data class SetDestinationSuccessEvent(
    val routeDurationInMilliseconds: Long
) : AdhocEvent()

/**
 * Event created each time when a presence message is received.
 * Should be created only from within the [CorePublisher].
 */
internal data class PresenceMessageEvent(
    val trackable: Trackable,
    val presenceMessage: PresenceMessage
) : AdhocEvent()

/**
 * Event created when the location engine resolution should be changed.
 * Should be created only from within the [CorePublisher].
 */
internal class ChangeLocationEngineResolutionEvent : AdhocEvent()

/**
 * Event created when the navigation routing profile should be changed.
 */
internal data class ChangeRoutingProfileEvent(
    val routingProfile: RoutingProfile
) : AdhocEvent()

/**
 * Event created each time the Ably connection state changes.
 * Should be created only from within the [CorePublisher].
 */
internal data class AblyConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange
) : AdhocEvent()

/**
 * Event created each time the trackable Ably channel state changes.
 * Should be created only from within the [CorePublisher].
 */
internal data class ChannelConnectionStateChangeEvent(
    val connectionStateChange: ConnectionStateChange,
    val trackableId: String
) : AdhocEvent()
