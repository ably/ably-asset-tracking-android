package com.ably.tracking.example.publisher

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase

/**
 * Example of a simple custom logs handler for the Logback library.
 * For more information see http://logback.qos.ch/manual/appenders.html
 */
class CustomLogsAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    override fun append(event: ILoggingEvent?) {
        if (event == null) {
            return
        }

        // TODO log the message somewhere meaningful for your app
        // e.g. FirebaseCrashlytics.getInstance().log("${event.level}: ${event.message}");
    }
}
