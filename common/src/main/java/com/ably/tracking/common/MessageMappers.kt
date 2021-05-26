package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.google.gson.Gson
import io.ably.lib.types.Message
import io.ably.lib.types.PresenceMessage

fun PresenceMessage.getPresenceData(gson: Gson): PresenceData =
    gson.fromJson(data as String, PresenceDataMessage::class.java).toTracking()

fun PresenceDataMessage.toTracking(): PresenceData =
    PresenceData(type, resolution?.toTracking())

fun PresenceData.toMessage(): PresenceDataMessage =
    PresenceDataMessage(type, resolution?.toMessage())

fun ResolutionMessage.toTracking(): Resolution =
    Resolution(accuracy.toTracking(), desiredInterval, minimumDisplacement)

fun Resolution.toMessage(): ResolutionMessage =
    ResolutionMessage(accuracy.toMessage(), desiredInterval, minimumDisplacement)

fun AccuracyMessage.toTracking(): Accuracy = when (this) {
    AccuracyMessage.MINIMUM -> Accuracy.MINIMUM
    AccuracyMessage.LOW -> Accuracy.LOW
    AccuracyMessage.BALANCED -> Accuracy.BALANCED
    AccuracyMessage.HIGH -> Accuracy.HIGH
    AccuracyMessage.MAXIMUM -> Accuracy.MAXIMUM
}

fun Accuracy.toMessage(): AccuracyMessage = when (this) {
    Accuracy.MINIMUM -> AccuracyMessage.MINIMUM
    Accuracy.LOW -> AccuracyMessage.LOW
    Accuracy.BALANCED -> AccuracyMessage.BALANCED
    Accuracy.HIGH -> AccuracyMessage.HIGH
    Accuracy.MAXIMUM -> AccuracyMessage.MAXIMUM
}

fun EnhancedLocationUpdate.toJson(gson: Gson): String =
    gson.toJson(
        EnhancedLocationUpdateMessage(
            location.toGeoJson(),
            skippedLocations.map { it.toGeoJson() },
            intermediateLocations.map { it.toGeoJson() },
            type.toMessage()
        )
    )

fun Message.getEnhancedLocationUpdate(gson: Gson): EnhancedLocationUpdate =
    gson.fromJson(data as String, EnhancedLocationUpdateMessage::class.java)
        .let { message ->
            EnhancedLocationUpdate(
                message.location.toLocation(),
                message.skippedLocations.map { it.toLocation() },
                message.intermediateLocations.map { it.toLocation() },
                message.type.toTracking()
            )
        }

fun LocationUpdateTypeMessage.toTracking(): LocationUpdateType =
    when (this) {
        LocationUpdateTypeMessage.PREDICTED -> LocationUpdateType.PREDICTED
        LocationUpdateTypeMessage.ACTUAL -> LocationUpdateType.ACTUAL
    }

fun LocationUpdateType.toMessage(): LocationUpdateTypeMessage =
    when (this) {
        LocationUpdateType.PREDICTED -> LocationUpdateTypeMessage.PREDICTED
        LocationUpdateType.ACTUAL -> LocationUpdateTypeMessage.ACTUAL
    }
