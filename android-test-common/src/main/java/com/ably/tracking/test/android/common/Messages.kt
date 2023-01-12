package com.ably.tracking.test.android.common

import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import java.io.ByteArrayOutputStream

/**
 * Ably protocol messages
 */
object Message {

    /**
     * Construct a map containing Ably ErrorInfo data
     */
    private fun errorInfo(
        code: Int,
        statusCode: Int,
        message: String
    ) = mapOf(
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
    ) = mapOf(
        "action" to 2,
        "msgSerial" to msgSerial,
        "count" to count,
        "error" to errorInfo(errorCode, errorStatusCode, errorMessage)
    )
}

/**
 * Write a string-keyed map to a ByteArray using MsgPack
 */
fun Map<String, Any?>.pack(): ByteArray {
    val out = ByteArrayOutputStream()
    out.use {
        MessagePack.newDefaultPacker(out).use {
            packMap(this, it)
        }
    }
    return out.toByteArray()
}

/**
 * Recursively MsgPack string maps to an open MessagePacker instance
 */
@Suppress("UNCHECKED_CAST")
internal fun packMap(value: Map<String, Any?>, packer: MessagePacker) {
    packer.packMapHeader(value.size)

    value.forEach {
        packer.packString(it.key)
        if (it.value == null) {
            packer.packNil()
        } else {
            when (val v = it.value) {
                is String -> packer.packString(v)
                is Int -> packer.packInt(v)
                is Long -> packer.packLong(v)
                is Boolean -> packer.packBoolean(v)
                is Double -> packer.packDouble(v)
                is Float -> packer.packFloat(v)
                is Map<*, *> -> packMap(v as Map<String, Any?>, packer)
            }
        }
    }
}
