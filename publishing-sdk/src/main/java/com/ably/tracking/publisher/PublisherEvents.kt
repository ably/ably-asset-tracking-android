package com.ably.tracking.publisher

import android.location.Location
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.ConnectionStateChangeHandler
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.GeoJsonMessage
import io.ably.lib.realtime.Channel
import io.ably.lib.types.PresenceMessage

internal sealed class PublisherEvent

internal data class StopPublisherEvent(
    val handler: ResultHandler
) : PublisherEvent()

internal class PublisherStoppedEvent() : PublisherEvent()

internal class AblyStoppedEvent() : PublisherEvent()

internal class MapboxStoppedEvent() : PublisherEvent()

internal class StartPublisherEvent : PublisherEvent()

internal data class SuccessEvent(
    val onSuccess: () -> Unit
) : PublisherEvent()

internal data class ErrorEvent(
    val exception: Exception,
    val onError: (Exception) -> Unit
) : PublisherEvent()

internal data class AddTrackableEvent(
    val trackable: Trackable,
    val onSuccess: () -> Unit,
    val onError: (Exception) -> Unit
) : PublisherEvent()

internal data class TrackTrackableEvent(
    val trackable: Trackable,
    val onSuccess: () -> Unit,
    val onError: (Exception) -> Unit
) : PublisherEvent()

internal data class RemoveTrackableEvent(
    val trackable: Trackable,
    val onSuccess: (wasPresent: Boolean) -> Unit,
    val onError: (Exception) -> Unit
) : PublisherEvent()

internal data class JoinPresenceSuccessEvent(
    val trackable: Trackable,
    val channel: Channel,
    val onSuccess: () -> Unit
) : PublisherEvent()

internal data class TrackableReadyToTrackEvent(
    val trackable: Trackable,
    val onSuccess: () -> Unit
) : PublisherEvent()

internal data class ClearActiveTrackableEvent(
    val trackable: Trackable,
    val onSuccess: () -> Unit
) : PublisherEvent()

internal data class RawLocationChangedEvent(
    val location: Location,
    val geoJsonMessage: GeoJsonMessage
) : PublisherEvent()

internal data class EnhancedLocationChangedEvent(
    val location: Location,
    val geoJsonMessages: List<GeoJsonMessage>
) : PublisherEvent()

internal data class SetDestinationEvent(
    val destination: Destination
) : PublisherEvent()

internal class RefreshResolutionPolicyEvent : PublisherEvent()

internal data class SetDestinationSuccessEvent(
    val routeDurationInMilliseconds: Long
) : PublisherEvent()

internal data class PresenceMessageEvent(
    val trackable: Trackable,
    val presenceMessage: PresenceMessage
) : PublisherEvent()

internal class ChangeLocationEngineResolutionEvent : PublisherEvent()

internal data class AblyStatusChangedEvent(
    val connectionState: ConnectionStateChange,
    val handler: ConnectionStateChangeHandler?
) : PublisherEvent()
