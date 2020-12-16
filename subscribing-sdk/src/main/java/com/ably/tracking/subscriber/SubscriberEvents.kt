package com.ably.tracking.subscriber

import android.location.Location
import com.ably.tracking.Resolution
import io.ably.lib.types.PresenceMessage

internal sealed class SubscriberEvent

internal class StopSubscriberEvent : SubscriberEvent()

internal data class SuccessEvent(
    val onSuccess: () -> Unit
) : SubscriberEvent()

internal data class ErrorEvent(
    val exception: Exception,
    val onError: (Exception) -> Unit
) : SubscriberEvent()

internal data class RawLocationReceivedEvent(
    val location: Location
) : SubscriberEvent()

internal data class EnhancedLocationReceivedEvent(
    val location: Location
) : SubscriberEvent()

internal data class PresenceMessageEvent(
    val presenceMessage: PresenceMessage
) : SubscriberEvent()

internal data class ChangeResolutionEvent(
    val resolution: Resolution,
    val onSuccess: () -> Unit,
    val onError: (Exception) -> Unit
) : SubscriberEvent()
