package com.ably.tracking.example.publisher

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase

/**
 * Example of a simple custom logs handler for the logback library.
 * For more information see http://logback.qos.ch/manual/appenders.html
 */
class CustomLogsAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    override fun append(event: ILoggingEvent?) {
        if (event == null) {
            return
        }
        when (event.level.levelInt) {
            Level.TRACE_INT -> {
                // handle TRACE/VERBOSE logs
            }
            Level.DEBUG_INT -> {
                // handle DEBUG logs
            }
            Level.INFO_INT -> {
                // handle INFO logs
            }
            Level.WARN_INT -> {
                // handle WARN logs
            }
            Level.ERROR_INT -> {
                // handle ERROR logs
            }
        }
    }
}
