package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

internal typealias AddTrackableResult = StateFlow<TrackableState>
internal typealias AddTrackableCallbackFunction = ResultCallbackFunction<AddTrackableResult>

/**
 * How long should we wait before re-queueing the work if starting or stopping Ably connection is in progress.
 * If after the delay the Ably connection process is still in progress the work will be re-queued again.
 */
private const val WORK_DELAY_IN_MILLISECONDS = 200L

/**
 * This worker establishes Ably connection and enters trackable to specific channel.
 * First of three steps required to add a trackable, the next step is [SubscribeToTrackablePresenceMessagesWorker] and [AddTrackableToPublisherWorker] afterwards.
 */
internal class PrepareConnectionForTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    private val ably: Ably
) : Worker<PublisherProperties, WorkerSpecification> {
    /**
     * Whether the worker is delaying its work.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isDelayingWork: Boolean = false

    /**
     * Whether the SDK is connected to Ably.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isConnectedToAbly: Boolean = true

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        when {
            properties.duplicateTrackableGuard.isCurrentlyAddingTrackable(trackable) -> {
                properties.duplicateTrackableGuard.saveDuplicateAddHandler(trackable, callbackFunction)
            }
            properties.trackables.contains(trackable) -> {
                val trackableFlow = properties.trackableStateFlows[trackable.id]!!
                callbackFunction(Result.success(trackableFlow))
            }
            properties.state == PublisherState.CONNECTING || properties.state == PublisherState.DISCONNECTING -> {
                doAsyncWork {
                    isDelayingWork = true
                    // delay work until Ably connection manipulation ends
                    delay(WORK_DELAY_IN_MILLISECONDS)
                    postWork(createWorkerSpecificationToDelay())
                }
            }
            else -> {
                val isAddingTheFirstTrackable = properties.hasNoTrackablesAddingOrAdded
                if (isAddingTheFirstTrackable) {
                    properties.state = PublisherState.CONNECTING
                }
                properties.duplicateTrackableGuard.startAddingTrackable(trackable)
                doAsyncWork {
                    createConnection(postWork, isAddingTheFirstTrackable, properties.presenceData)
                }
            }
        }
        return properties
    }

    private suspend fun createConnection(
        postWork: (WorkerSpecification) -> Unit,
        isAddingTheFirstTrackable: Boolean,
        presenceData: PresenceData
    ) {
        if (isAddingTheFirstTrackable) {
            isConnectedToAbly = false
            val startAblyConnectionResult = ably.startConnection()
            if (startAblyConnectionResult.isFailure) {
                val workerSpecification = createAddTrackableFailedWorker(
                    startAblyConnectionResult.exceptionOrNull(),
                    isConnectedToAbly = false
                )
                postWork(workerSpecification)
                return
            }
            isConnectedToAbly = true
        }
        val connectResult = ably.connect(
            trackableId = trackable.id,
            presenceData = presenceData,
            willPublish = true,
        )
        val workerSpecification = if (connectResult.isSuccess) {
            createConnectionCreatedWorker()
        } else {
            createAddTrackableFailedWorker(connectResult.exceptionOrNull(), isConnectedToAbly = true)
        }
        postWork(workerSpecification)
    }

    private fun createConnectionCreatedWorker() =
        WorkerSpecification.SubscribeToTrackablePresenceMessages(
            trackable,
            callbackFunction,
            presenceUpdateListener,
            channelStateChangeListener
        )

    private fun createAddTrackableFailedWorker(
        exception: Throwable?,
        isConnectedToAbly: Boolean
    ) = WorkerSpecification.AddTrackableFailed(
        trackable,
        callbackFunction,
        exception as Exception,
        isConnectedToAbly
    )

    private fun createWorkerSpecificationToDelay() =
        WorkerSpecification.PrepareConnectionForTrackable(
            trackable,
            callbackFunction,
            presenceUpdateListener,
            channelStateChangeListener
        )

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        if (isDelayingWork) {
            postWork(createWorkerSpecificationToDelay())
        } else {
            postWork(createAddTrackableFailedWorker(exception, isConnectedToAbly))
        }
    }
}
