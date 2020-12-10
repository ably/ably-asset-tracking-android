package com.ably.tracking.publisher

import android.location.Location
import com.ably.tracking.common.GeoJsonMessage
import io.ably.lib.realtime.Channel

internal sealed class PublisherEvent

internal class StopPublisherEvent : PublisherEvent()

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
