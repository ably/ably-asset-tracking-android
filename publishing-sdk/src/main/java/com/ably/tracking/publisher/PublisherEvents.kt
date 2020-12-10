package com.ably.tracking.publisher

internal sealed class PublisherEvent

internal class StopPublisherEvent : PublisherEvent()

internal class StartPublisherEvent : PublisherEvent()

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
