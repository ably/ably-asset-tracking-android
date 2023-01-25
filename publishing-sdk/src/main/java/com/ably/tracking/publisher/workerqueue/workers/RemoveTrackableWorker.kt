package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class RemoveTrackableWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val callbackFunction: ResultCallbackFunction<Boolean>
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        when {
            properties.trackables.contains(trackable) -> {
                doAsyncWork {
                    // Leave Ably channel.
                    ably.disconnect(trackable.id, properties.presenceData)
                    postWork(buildDisconnectSuccessWorkerSpecification(postWork))
                }
            }
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.trackableRemovalGuard.markForRemoval(trackable, callbackFunction)
            }
            else -> {
                doAsyncWork {
                    callbackFunction(Result.success(false))
                }
            }
        }
        return properties
    }

    private fun buildDisconnectSuccessWorkerSpecification(postWork: (WorkerSpecification) -> Unit) =
        WorkerSpecification.DisconnectSuccess(
            trackable = trackable,
            callbackFunction = {
                if (it.isSuccess) {
                    callbackFunction(Result.success(true))
                } else {
                    callbackFunction(Result.failure(it.exceptionOrNull()!!))
                }
            },
            shouldRecalculateResolutionCallback = {
                postWork(WorkerSpecification.ChangeLocationEngineResolution)
            }
        )

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }
}
