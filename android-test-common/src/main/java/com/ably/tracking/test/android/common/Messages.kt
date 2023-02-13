package com.ably.tracking.test.android.common

import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.value.Value
import java.io.ByteArrayOutputStream

/**
 * Ably protocol messages
 */
object Message {

    /**
     * Ably protocol action codes
     */
    enum class Action(val code: Int) {
        ACK(1),
        CONNECTED(4),
        ATTACH(10),
        ATTACHED(11),
        DETACH(12),
        PRESENCE(14),
        SYNC(16),
    }

    /**
     * Types of Presence messages
     */
    enum class PresenceAction(val code: Int) {
        ENTER(2),
        UPDATE(4)
    }

    /**
     * Construct a map containing Ably ErrorInfo data
     */
    private fun errorInfo(
        code: Int,
        statusCode: Int,
        message: String
    ): Map<String?, Any?> = mapOf(
        "code" to code,
        "statusCode" to statusCode,
        "message" to message
    )

    /**
     * Builds a NACK Ably protocol message as a string map
     */
    fun nack(
        msgSerial: Int,
        count: Int,
        errorCode: Int,
        errorStatusCode: Int,
        errorMessage: String
    ): Map<String?, Any?> = mapOf(
        "action" to 2,
        "msgSerial" to msgSerial,
        "count" to count,
        "error" to errorInfo(errorCode, errorStatusCode, errorMessage)
    )
}

/**
 * Check to see if given Message is of specified action type
 */
fun Map<String?, Any?>.isAction(action: Message.Action) =
    action.code == (this["action"] as Int)

/**
 * Check to see if this message is a presence message with the given action
 */
@Suppress("UNCHECKED_CAST")
fun Map<String?, Any?>.isPresenceAction(action: Message.PresenceAction) =
    this.containsKey("presence") &&
        this["presence"].let {
            val presenceMap = it as List<Map<String?, Any?>>
            presenceMap.isNotEmpty() &&
                presenceMap[0]["action"] == action.code
        }

/**
 * Write a string-keyed map to a ByteArray using MsgPack
 */
fun Map<String?, Any?>.pack(): ByteArray {
    val out = ByteArrayOutputStream()
    out.use {
        MessagePack.newDefaultPacker(out).use {
            packValue(this, it)
        }
    }
    return out.toByteArray()
}

/**
 * Pack values recursively into a MsgPack packer
 */
@Suppress("UNCHECKED_CAST")
internal fun packValue(value: Any?, packer: MessagePacker) {
    if (value == null) {
        packer.packNil()
    } else {
        when (value) {
            is String -> packer.packString(value)
            is Int -> packer.packInt(value)
            is Long -> packer.packLong(value)
            is Boolean -> packer.packBoolean(value)
            is Double -> packer.packDouble(value)
            is Float -> packer.packFloat(value)
            is List<*> -> packList(value, packer)
            is Map<*, *> -> packMap(value as Map<Any, Any?>, packer)
            else -> throw Exception("packValue: Unimplemented MsgPack conversion")
        }
    }
}

/**
 * Pack an array of values into MsgPack
 */
internal fun packList(value: List<Any?>, packer: MessagePacker) {
    packer.packArrayHeader(value.size)
    value.forEach {
        packValue(it, packer)
    }
}

/**
 * Packs a map of values into a MsgPack buffer
 */
internal fun packMap(value: Map<Any, Any?>, packer: MessagePacker) {
    packer.packMapHeader(value.size)
    value.forEach {
        packValue(it.key, packer)
        packValue(it.value, packer)
    }
}

/**
 * Unpack a byte array of MsgPack data to a String-keyed map, as expected
 * in all Ably protocol messages at the top level.
 */
fun ByteArray.unpack(): Map<String?, Any?> {
    try {
        MessagePack.newDefaultUnpacker(this).use {
            val map = it.unpackValue()?.asMapValue()?.map()
            if (map != null) {
                return unpackStringMap(map)
            } else {
                throw Exception("unpack: ByteArray did not contain MsgPack Map")
            }
        }
    } catch (e: Exception) {
        testLogD("MsgPack Error: $e", e)
        throw(e)
    }
}

/**
 * Unpack a MsgPack array to Kotlin-usable values
 */
fun unpackList(listVal: List<Value?>) =
    listVal.map(::unpackValue)

/**
 * Unpack a MsgPack map to a Kotlin map with String keys
 * and idiomatic values
 */
fun unpackStringMap(mapVal: Map<Value?, Value?>) =
    mapVal.map {
        it.key?.asStringValue()?.toString() to unpackValue(it.value)
    }.toMap()

/**
 * Recursively unpack a value from the MsgPack wrapper value, so that
 * it is usable from Kotlin code
 */
fun unpackValue(value: Value?): Any? =
    when {
        value == null -> null
        value.isStringValue -> value.asStringValue().toString()
        value.isIntegerValue -> value.asIntegerValue().toInt()
        value.isFloatValue -> value.asFloatValue().toFloat()
        value.isBooleanValue -> value.asBooleanValue().boolean
        value.isArrayValue -> unpackList(value.asArrayValue().list())
        value.isMapValue -> unpackStringMap(value.asMapValue().map())
        else -> throw Exception("unpackValue: Unimplemented MsgPack conversion")
    }
