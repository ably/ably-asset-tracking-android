package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.AddTrackableEvent
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.flow.StateFlow

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val ably: Ably
) : SyncAsyncWorker {
    override val event: Request<*>
        get() = AddTrackableEvent(trackable, callbackFunction)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        return when {
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, callbackFunction)
                SyncAsyncResult()
            }
            properties.trackables.contains(trackable) -> {
                SyncAsyncResult(AddTrackableWorkResult.AlreadyIn(properties.trackableStateFlows[trackable.id]!!, callbackFunction))
            }
            else -> {
                properties.duplicateTrackableGuard.startAddingTrackable(trackable)
                val presenceData = properties.presenceData.copy()

                SyncAsyncResult(
                    asyncWork = {
                        val connectResult = ably.connect(
                            trackableId = trackable.id,
                            presenceData = presenceData,
                            willPublish = true
                        )
                        if (connectResult.isSuccess) {
                            AddTrackableWorkResult.Success(trackable, callbackFunction)
                        } else {
                            AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull(), callbackFunction)
                        }
                    }
                )
            }
        }
    }
}
