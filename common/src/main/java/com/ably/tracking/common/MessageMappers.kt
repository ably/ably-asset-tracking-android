package com.ably.tracking.common

import com.ably.tracking.EnhancedLocationUpdate
import com.google.gson.Gson
import io.ably.lib.types.Message
import io.ably.lib.types.PresenceMessage

fun PresenceMessage.getPresenceData(gson: Gson): PresenceData =
    gson.fromJson(data as String, PresenceData::class.java)

fun EnhancedLocationUpdate.toJson(gson: Gson): String =
    gson.toJson(EnhancedLocationUpdateMessage(location.toGeoJson(), intermediateLocations.map { it.toGeoJson() }, type))

fun Message.getEnhancedLocationUpdate(gson: Gson): EnhancedLocationUpdate =
    gson.fromJson(data as String, EnhancedLocationUpdateMessage::class.java)
        .let { message ->
            EnhancedLocationUpdate(
                message.location.toLocation(),
                message.intermediateLocations.map { it.toLocation() },
                message.type
            )
        }
