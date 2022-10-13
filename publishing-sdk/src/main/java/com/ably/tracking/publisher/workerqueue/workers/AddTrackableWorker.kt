package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

internal typealias AddTrackableResult = StateFlow<TrackableState>
internal typealias AddTrackableCallbackFunction = ResultCallbackFunction<AddTrackableResult>

/**
 * How long should we wait before re-queueing the work if Ably connection manipulation is in progress.
 */
private const val WORK_DELAY_IN_MILLISECONDS = 200L

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    private val ably: Ably
) : Worker {
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
                if (properties.isConnectingToAbly || properties.isStoppingAbly) {
                    return SyncAsyncResult(
                        asyncWork = {
                            // delay work until Ably connection manipulation ends
                            delay(WORK_DELAY_IN_MILLISECONDS)
                            AddTrackableWorkResult.WorkDelayed(trackable, callbackFunction, presenceUpdateListener, channelStateChangeListener)
                        }
                    )
                }
                val isAddingTheFirstTrackable = properties.hasNoTrackablesAddingOrAdded
                if (isAddingTheFirstTrackable) {
                    properties.isConnectingToAbly = true
                }
                properties.duplicateTrackableGuard.startAddingTrackable(trackable)
                val presenceData = properties.presenceData.copy()

                SyncAsyncResult(
                    asyncWork = {
                        if (isAddingTheFirstTrackable) {
                            val startAblyConnectionResult = ably.startConnection()
                            if (startAblyConnectionResult.isFailure) {
                                AddTrackableWorkResult.Fail(trackable, startAblyConnectionResult.exceptionOrNull(), callbackFunction)
                            }
                        }
                        val connectResult = ably.connect(
                            trackableId = trackable.id,
                            presenceData = presenceData,
                            willPublish = true,
                        )
                        if (connectResult.isSuccess) {
                            AddTrackableWorkResult.Success(trackable, callbackFunction, presenceUpdateListener, channelStateChangeListener)
                        } else {
                            AddTrackableWorkResult.Fail(trackable, connectResult.exceptionOrNull(), callbackFunction)
                        }
                    }
                )
            }
        }
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
