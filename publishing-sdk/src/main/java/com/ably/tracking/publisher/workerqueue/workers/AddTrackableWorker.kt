package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal typealias AddTrackableResult = StateFlow<TrackableState>
internal typealias AddTrackableCallbackFunction = ResultCallbackFunction<AddTrackableResult>

/**
 * How long should we wait before re-queueing the work if starting or stopping Ably connection is in progress.
 * If after the delay the Ably connection process is still in progress the work will be re-queued again.
 */
private const val WORK_DELAY_IN_MILLISECONDS = 200L

internal class AddTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    private val ably: Ably,
    private val publisherInteractor: PublisherInteractor,
    private val hooks: DefaultCorePublisher.Hooks
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
            properties.trackables.contains(trackable) -> {
                val trackableFlow = properties.trackableStateFlows[trackable.id]!!
                callbackFunction(Result.success(trackableFlow))
            }
            properties.state == PublisherState.DISCONNECTING -> {
                doAsyncWork {
                    isDelayingWork = true
                    // delay work until Ably disconnection ends
                    delay(WORK_DELAY_IN_MILLISECONDS)
                    postWork(createWorkerSpecificationToDelay())
                }
            }
            else -> {
                val isAddingTheFirstTrackable = properties.hasNoTrackablesAdded
                if (isAddingTheFirstTrackable) {
                    properties.state = PublisherState.CONNECTING
                }

                // Add the trackable to the publisher and return success immediately
                addTrackableToPublisherAndResolveResolution(properties)
                notifyTrackableAddSuccess(createTrackableStateFlows(properties))

                // Create the ably connection as required
                doAsyncWork {
                    createConnection(postWork, isAddingTheFirstTrackable, properties.presenceData)
                }
            }
        }
        return properties
    }

    private fun addTrackableToPublisherAndResolveResolution(properties: PublisherProperties) {
        properties.trackables.add(trackable)
        publisherInteractor.updateTrackables(properties)
        publisherInteractor.resolveResolution(trackable, properties)
        hooks.trackables?.onTrackableAdded(trackable)
    }

    private fun createTrackableStateFlows(properties: PublisherProperties): MutableStateFlow<TrackableState> {
        val trackableState = properties.trackableStates[trackable.id] ?: TrackableState.Offline()
        val trackableStateFlow = properties.trackableStateFlows[trackable.id] ?: MutableStateFlow(trackableState)

        properties.trackableStateFlows[trackable.id] = trackableStateFlow
        publisherInteractor.updateTrackableStateFlows(properties)
        properties.trackableStates[trackable.id] = trackableState
        properties.trackableSubscribedToPresenceFlags[trackable.id] = false
        return trackableStateFlow
    }

    private fun notifyTrackableAddSuccess(stateFlow: MutableStateFlow<TrackableState>) {
        val successResult = Result.success(stateFlow.asStateFlow())
        callbackFunction(successResult)
    }

    private suspend fun createConnection(
        postWork: (WorkerSpecification) -> Unit,
        isAddingTheFirstTrackable: Boolean,
        presenceData: PresenceData
    ) {
        if (isAddingTheFirstTrackable) {
            isConnectedToAbly = false
            val startAblyConnectionResult = ably.startConnection()

            if (startAblyConnectionResult.isFatalAblyFailure()) {
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

        if (connectResult.isFatalAblyFailure()) {
            postWork(
                createAddTrackableFailedWorker(
                    connectResult.exceptionOrNull(),
                    isConnectedToAbly = true
                )
            )
            return
        }

        // If the connection result is successful, then we've entered presence
        postWork(createEnterPresenceWorker(connectResult.isSuccess))
        postWork(createSubscribeToPresenceWorker())
        postWork(createConnectionReadyWorker())
    }

    private fun createEnterPresenceWorker(enteredPresence: Boolean) =
        WorkerSpecification.EnterPresence(trackable, enteredPresence)

    private fun createSubscribeToPresenceWorker() =
        WorkerSpecification.SubscribeToPresence(trackable, presenceUpdateListener)

    private fun createConnectionReadyWorker() =
        WorkerSpecification.ConnectionReady(
            trackable,
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
        WorkerSpecification.AddTrackable(
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

    override fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecification) -> Unit
    ) {
        if (isDelayingWork) {
            postWork(createWorkerSpecificationToDelay())
        } else {
            postWork(createAddTrackableFailedWorker(exception, isConnectedToAbly))
        }
    }
}
