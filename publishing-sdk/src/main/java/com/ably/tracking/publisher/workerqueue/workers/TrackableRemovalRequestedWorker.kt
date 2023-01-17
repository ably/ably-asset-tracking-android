package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.flow.StateFlow

internal class TrackableRemovalRequestedWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val ably: Ably,
    private val result: Result<Unit>
) : Worker<PublisherProperties, WorkerSpecification> {
    /**
     * Whether the worker is also performing disconnecting.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isDisconnecting: Boolean = false

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (result.isSuccess) {
            properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))
        } else {
            properties.trackableRemovalGuard.removeMarked(trackable, Result.failure(result.exceptionOrNull()!!))
        }
        callbackFunction(Result.failure(RemoveTrackableRequestedException()))
        properties.duplicateTrackableGuard.finishAddingTrackable(
            trackable,
            Result.failure(RemoveTrackableRequestedException())
        )
        val removedTheLastTrackable = properties.hasNoTrackablesAddingOrAdded
        if (removedTheLastTrackable) {
            properties.state = PublisherState.DISCONNECTING
            doAsyncWork {
                isDisconnecting = true
                ably.stopConnection()
                postWork(WorkerSpecification.StoppingConnectionFinished)
            }
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        // Async work is an optional step that happens after the callback was called so it should not call the callback
        if (isDisconnecting) {
            // When async work fails we should make sure that the SDK state is not stuck in DISCONNECTING so we post a new worker
            postWork(WorkerSpecification.StoppingConnectionFinished)
        }
    }
}
