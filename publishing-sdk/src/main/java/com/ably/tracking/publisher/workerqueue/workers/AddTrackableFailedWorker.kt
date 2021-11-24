package com.ably.tracking.publisher.workerqueue.workers


import android.util.Log
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult

private const val TAG = "AddTrackableFailedWorke"
internal class AddTrackableFailedWorker(private val exception: Throwable?) : Worker {
    override fun doWork(publisherState: DefaultCorePublisher.State): SyncAsyncResult {
        Log.d(TAG, "doWork: ${exception?.message}")
        return SyncAsyncResult()
    }
}
