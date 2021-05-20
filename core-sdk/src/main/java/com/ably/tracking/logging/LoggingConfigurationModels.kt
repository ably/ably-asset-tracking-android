package com.ably.tracking.logging

interface LogHandler {
    fun logMessage(level: LogLevel, message: String, throwable: Throwable? = null)
}

enum class LogLevel(val levelInt: Int) {
    VERBOSE(1), INFO(2), DEBUG(3), WARN(4), ERROR(5)
}
