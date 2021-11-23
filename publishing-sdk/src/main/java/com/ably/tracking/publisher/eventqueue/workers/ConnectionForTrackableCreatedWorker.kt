package com.ably.tracking.publisher.eventqueue.workers

import android.util.Log
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.eventqueue.SyncAsyncResult
import com.ably.tracking.publisher.eventqueue.WorkResult

private const val TAG = "ConnectionForTrackableC"
class ConnectionForTrackableCreatedWorker(private val trackable: Trackable) :Worker {
    override fun doWork(): SyncAsyncResult {
        Log.d(TAG, "doWork: $trackable")
        return SyncAsyncResult()
    }
}
