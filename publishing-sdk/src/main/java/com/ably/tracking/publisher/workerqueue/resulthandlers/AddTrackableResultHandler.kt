package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.WorkerFactory
import com.ably.tracking.publisher.workerqueue.WorkerParams
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class AddTrackableResultHandler(
    private val workerFactory: WorkerFactory
) : WorkResultHandler<AddTrackableWorkResult> {

    override fun handle(workResult: AddTrackableWorkResult): Worker? {
        when (workResult) {
            is AddTrackableWorkResult.AlreadyIn -> workResult.callbackFunction(
                Result.success(workResult.trackableStateFlow)
            )

            is AddTrackableWorkResult.Fail ->
                return workerFactory.createWorker(
                    WorkerParams.AddTrackableFailed(
                        workResult.trackable, workResult.callbackFunction, workResult.exception as Exception, workResult.isConnectedToAbly
                    )
                )

            is AddTrackableWorkResult.Success ->
                return workerFactory.createWorker(
                    WorkerParams.ConnectionCreated(
                        workResult.trackable,
                        workResult.callbackFunction,
                        workResult.presenceUpdateListener,
                        workResult.channelStateChangeListener
                    )
                )

            is AddTrackableWorkResult.WorkDelayed ->
                return workerFactory.createWorker(
                    WorkerParams.AddTrackable(
                        workResult.trackable,
                        workResult.callbackFunction,
                        workResult.presenceUpdateListener,
                        workResult.channelStateChangeListener,
                    )
                )
        }
        return null
    }
}
