package com.ably.tracking.common.logging

import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel

fun LogHandler.v(message: String, throwable: Throwable? = null) {
    log(LogLevel.VERBOSE, message, throwable)
}

fun LogHandler.i(message: String, throwable: Throwable? = null) {
    log(LogLevel.INFO, message, throwable)
}

fun LogHandler.d(message: String, throwable: Throwable? = null) {
    log(LogLevel.DEBUG, message, throwable)
}

fun LogHandler.w(message: String, throwable: Throwable? = null) {
    log(LogLevel.WARN, message, throwable)
}

fun LogHandler.e(message: String, throwable: Throwable? = null) {
    log(LogLevel.ERROR, message, throwable)
}

fun LogHandler.e(throwable: Throwable) {
    log(LogLevel.ERROR, "", throwable)
}

private fun LogHandler.log(level: LogLevel, message: String, throwable: Throwable? = null) {
    val currentTimestamp = System.currentTimeMillis()
    logMessage(level, "$currentTimestamp: $message", throwable)
}
