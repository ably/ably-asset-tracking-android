package com.ably.tracking.publisher.workerqueue.workers

import android.util.Log
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult

private const val TAG = "ConnectionForTrackableC"
internal class ConnectionForTrackableCreatedWorker(private val trackable: Trackable) :Worker {
    override fun doWork(publisherState: DefaultCorePublisher.State): SyncAsyncResult {
        Log.d(TAG, "doWork: $trackable")
        return SyncAsyncResult()
    }
}
