package com.ably.tracking.subscriber

import android.location.Location

internal sealed class SubscriberEvent

internal class StopSubscriberEvent : SubscriberEvent()

internal data class RawLocationReceivedEvent(
    val location: Location
) : SubscriberEvent()

internal data class EnhancedLocationReceivedEvent(
    val location: Location
) : SubscriberEvent()
