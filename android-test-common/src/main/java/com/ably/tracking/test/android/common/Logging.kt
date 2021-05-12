package com.ably.tracking.test.android.common

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log

private val TAG = "PUBLISHING SDK IT"
private val encounteredThreadIds = HashSet<Long>()

@SuppressLint("LogConditional")
fun testLogD(message: String) {
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

    Log.d(TAG, "${Thread.currentThread().id}:  $message")
}
