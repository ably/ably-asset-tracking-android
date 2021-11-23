package com.ably.tracking.publisher.eventqueue.workers


import android.util.Log
import com.ably.tracking.publisher.eventqueue.SyncAsyncResult

private const val TAG = "AddTrackableFailedWorke"
class AddTrackableFailedWorker(private val exception: Throwable?) : Worker {
    override fun doWork(): SyncAsyncResult {
        Log.d(TAG, "doWork: ${exception?.message}")
        return SyncAsyncResult()
    }
}
