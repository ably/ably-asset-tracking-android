package com.ably.tracking.common

import com.google.gson.Gson
import com.google.gson.JsonParseException

/**
 * Deserializes the [json] and returns an object of type [classOfT] or a null if something goes wrong.
 */
fun <T> Gson.fromJsonOrNull(json: String?, classOfT: Class<T>): T? =
    try {
        fromJson(json, classOfT)
    } catch (e: JsonParseException) {
        null
    }
