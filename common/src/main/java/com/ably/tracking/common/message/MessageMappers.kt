package com.ably.tracking.common.message

import com.ably.tracking.Accuracy
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.common.PresenceData
import com.google.gson.Gson
import io.ably.lib.types.Message

/**
 * Returns parsed data or null if data is in wrong format.
 */
fun PresenceDataMessage.toTracking(): PresenceData? =
    type?.let { PresenceData(it, resolution?.toTracking()) }

fun PresenceData.toMessage(): PresenceDataMessage =
    PresenceDataMessage(type, resolution?.toMessage(), rawLocations)

fun ResolutionMessage.toTracking(): Resolution =
    Resolution(accuracy.toTracking(), desiredInterval, minimumDisplacement)

fun Resolution.toMessage(): ResolutionMessage =
    ResolutionMessage(accuracy.toMessage(), desiredInterval, minimumDisplacement)

fun Resolution.toMessageJson(gson: Gson): String =
    gson.toJson(this.toMessage())

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

fun LocationUpdate.toMessageJson(gson: Gson): String =
    gson.toJson(
        LocationUpdateMessage(
            location.toMessage(),
            skippedLocations.map { it.toMessage() },
        )
    )

fun EnhancedLocationUpdate.toMessageJson(gson: Gson): String =
    gson.toJson(
        EnhancedLocationUpdateMessage(
            location.toMessage(),
            skippedLocations.map { it.toMessage() },
            intermediateLocations.map { it.toMessage() },
            type.toMessage()
        )
    )

fun Message.getEnhancedLocationUpdate(gson: Gson): EnhancedLocationUpdate =
    gson.fromJson(data as String, EnhancedLocationUpdateMessage::class.java)
        .let { message ->
            EnhancedLocationUpdate(
                message.location.toTracking(),
                message.skippedLocations.map { it.toTracking() },
                message.intermediateLocations.map { it.toTracking() },
                message.type.toTracking()
            )
        }

fun Message.getRawLocationUpdate(gson: Gson): LocationUpdate =
    gson.fromJson(data as String, LocationUpdateMessage::class.java)
        .let { message ->
            LocationUpdate(
                message.location.toTracking(),
                message.skippedLocations.map { it.toTracking() },
            )
        }

fun Message.getResolution(gson: Gson): Resolution =
    gson.fromJson(data as String, ResolutionMessage::class.java).toTracking()

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

fun Location.toMessage(): LocationMessage =
    LocationMessage(
        GeoJsonTypes.FEATURE,
        LocationGeometry(GeoJsonTypes.POINT, listOf(longitude, latitude, altitude)),
        LocationProperties(
            accuracy,
            bearing,
            speed,
            time.toDouble() / MILLISECONDS_PER_SECOND
        )
    )

fun LocationMessage.toTracking(): Location =
    Location(
        longitude = geometry.coordinates[GEOMETRY_LONG_INDEX],
        latitude = geometry.coordinates[GEOMETRY_LAT_INDEX],
        altitude = geometry.coordinates[GEOMETRY_ALT_INDEX],
        accuracy = properties.accuracyHorizontal,
        bearing = properties.bearing,
        speed = properties.speed,
        time = (properties.time * MILLISECONDS_PER_SECOND).toLong()
    )

fun Message.getLocationMessages(gson: Gson): List<LocationMessage> =
    gson.fromJson(data as String, Array<LocationMessage>::class.java).toList()
