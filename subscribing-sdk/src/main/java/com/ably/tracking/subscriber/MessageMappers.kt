package com.ably.tracking.subscriber

import com.google.gson.Gson
import io.ably.lib.types.PresenceMessage

internal fun PresenceMessage.getData(gson: Gson): PresenceData =
    gson.fromJson(data as String, PresenceData::class.java)
