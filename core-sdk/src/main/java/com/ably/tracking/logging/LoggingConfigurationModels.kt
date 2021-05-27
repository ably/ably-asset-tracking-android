package com.ably.tracking.logging

/**
 * Simple interface that allows to handle logs sent from the SDK.
 */
interface LogHandler {
    /**
     * Gets called when a log message is sent from the SDK.
     * This method is called synchronously and can be called from any thread.
     * Don't run blocking code in this method as this can cause the SDK to malfunction.
     *
     * @param level The importance level of the message.
     * @param message The message text.
     * @param throwable Optional error object.
     */
    fun logMessage(level: LogLevel, message: String, throwable: Throwable? = null)
}

/**
 * Defines importance levels for log messages.
 */
enum class LogLevel(val levelInt: Int) {
    VERBOSE(1), INFO(2), DEBUG(3), WARN(4), ERROR(5)
}
