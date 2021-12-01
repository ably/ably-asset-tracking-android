package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultHandler
import com.ably.tracking.publisher.AddTrackableEvent
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.SyncAsyncResult
import kotlinx.coroutines.flow.StateFlow

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val handler: ResultHandler<StateFlow<TrackableState>>,
    private val ably: Ably
) : Worker {
    override val event: Request<*>
        get() = AddTrackableEvent(trackable,handler)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        return when {
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, handler)
                SyncAsyncResult()
            }
            properties.trackables.contains(trackable) -> {
                SyncAsyncResult(
                    AddTrackableWorkResult.AlreadyIn(properties.trackableStateFlows[trackable.id]!!, handler),
                    null
                )
            }
            else -> {
                properties.duplicateTrackableGuard.startAddingTrackable(trackable)
                val presenceData = properties.presenceData.copy()

                SyncAsyncResult(
                    syncWorkResult = null,
                    asyncWork = {
                        val connectResult = ably.suspendingConnect(
                            trackableId = trackable.id,
                            presenceData = presenceData,
                            willPublish = true
                        )
                        if (connectResult.isSuccess) {
                            AddTrackableWorkResult.Success(trackable, handler)
                        } else {
                            AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull(), handler)
                        }
                    }
                )
            }
        }
    }
}
