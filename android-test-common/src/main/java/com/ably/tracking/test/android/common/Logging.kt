package com.ably.tracking.test.android.common

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel

object Logging {
    /**
     * Redirect Ably and AAT logging to testLogD
     */
    val aatDebugLogger = object : LogHandler {
        override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
            if (throwable != null) {
                testLogD("$message $throwable")
            } else {
                testLogD(message)
            }
        }
    }

    val ablyJavaDebugLogger = io.ably.lib.util.Log.LogHandler { _, _, msg, tr ->
        aatDebugLogger.logMessage(LogLevel.DEBUG, msg!!, tr)
    }

    private const val TAG = "AAT SDK IT"
    private val encounteredThreadIds = HashSet<Long>()

    @SuppressLint("LogNotTimber", "LogConditional")
    fun testLogD(message: String, exc: Throwable? = null) {
        val thread = Thread.currentThread()
        val currentThreadId = thread.id
        if (!encounteredThreadIds.contains(currentThreadId)) {
            val currentThreadLooper = Looper.myLooper()

            val looperDescription =
                if (null != currentThreadLooper)
                    if (currentThreadLooper == Looper.getMainLooper())
                        "main Looper"
                    else
                        "has Looper (not main)"
                else "no Looper"

            Log.d(TAG, "THREAD $currentThreadId is '${thread.name}' [$looperDescription]")
            encounteredThreadIds.add(currentThreadId)
        }

        if (exc != null) {
            Log.d(TAG, "${Thread.currentThread().id}:  $message", exc)
        } else {
            Log.d(TAG, "${Thread.currentThread().id}:  $message")
        }
    }
}
