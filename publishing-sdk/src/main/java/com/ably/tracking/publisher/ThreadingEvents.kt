package com.ably.tracking.publisher

internal sealed class ThreadingEvent

internal data class TrackTrackableEvent(
    val trackable: Trackable,
    val onSuccess: () -> Unit,
    val onError: (Exception) -> Unit
) : ThreadingEvent()
