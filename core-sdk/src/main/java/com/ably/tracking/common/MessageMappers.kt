package com.ably.tracking.common

import com.google.gson.Gson
import io.ably.lib.types.PresenceMessage

fun PresenceMessage.getData(gson: Gson): PresenceData =
    gson.fromJson(data as String, PresenceData::class.java)
