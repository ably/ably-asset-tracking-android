package com.ably.tracking.publisher

import android.location.Location
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.GeoJsonMessage
import io.ably.lib.realtime.Channel
import io.ably.lib.types.PresenceMessage

internal sealed class Event

internal class StopEvent : Event()

internal class StartEvent : Event()

internal data class AddTrackableEvent(
    val trackable: Trackable,
    val handler: ResultHandler<Unit>
) : Event()

internal data class TrackTrackableEvent(
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
    val channel: Channel,
    val handler: ResultHandler<Unit>
) : Event()

internal data class RawLocationChangedEvent(
    val location: Location,
    val geoJsonMessage: GeoJsonMessage
) : Event()

internal data class EnhancedLocationChangedEvent(
    val location: Location,
    val geoJsonMessages: List<GeoJsonMessage>
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
