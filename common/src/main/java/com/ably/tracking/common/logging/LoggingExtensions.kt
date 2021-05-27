package com.ably.tracking.common.logging

import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel

fun LogHandler.v(message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.VERBOSE, message, throwable)
}

fun LogHandler.i(message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.INFO, message, throwable)
}

fun LogHandler.d(message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.DEBUG, message, throwable)
}

fun LogHandler.w(message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.WARN, message, throwable)
}

fun LogHandler.e(message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.ERROR, message, throwable)
}

fun LogHandler.e(throwable: Throwable) {
    logMessage(LogLevel.ERROR, "", throwable)
}
