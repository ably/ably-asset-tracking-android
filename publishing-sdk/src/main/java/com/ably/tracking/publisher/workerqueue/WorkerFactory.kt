package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.Location
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.TimeProvider
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionCreatedWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionReadyWorker
import com.ably.tracking.publisher.workerqueue.workers.DisconnectSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.EnhancedLocationChangedWorker
import com.ably.tracking.publisher.workerqueue.workers.RefreshResolutionPolicyWorker
import com.ably.tracking.publisher.workerqueue.workers.RetrySubscribeToPresenceSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.RetrySubscribeToPresenceWorker
import com.ably.tracking.publisher.workerqueue.workers.StopWorker
import com.ably.tracking.publisher.workerqueue.workers.StoppingConnectionFinishedWorker
import com.ably.tracking.publisher.workerqueue.workers.TrackableRemovalRequestedWorker
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.flow.StateFlow

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal interface WorkerFactory {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerParams].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerParams): Worker
}

internal class DefaultWorkerFactory(
    private val ably: Ably,
    private val hooks: DefaultCorePublisher.Hooks,
    private val corePublisher: CorePublisher,
    private val resolutionPolicy: ResolutionPolicy,
    private val mapbox: Mapbox,
    private val timeProvider: TimeProvider,
    private val logHandler: LogHandler?,
) : WorkerFactory {
    override fun createWorker(params: WorkerParams): Worker {
        return when (params) {
            is WorkerParams.AddTrackable -> AddTrackableWorker(
                params.trackable,
                params.callbackFunction,
                params.presenceUpdateListener,
                params.channelStateChangeListener,
                ably,
            )
            is WorkerParams.AddTrackableFailed -> AddTrackableFailedWorker(
                params.trackable,
                params.callbackFunction,
                params.exception,
                params.isConnectedToAbly,
                ably,
            )
            is WorkerParams.ConnectionCreated -> ConnectionCreatedWorker(
                params.trackable,
                params.callbackFunction,
                ably,
                logHandler,
                params.presenceUpdateListener,
                params.channelStateChangeListener,
            )
            is WorkerParams.ConnectionReady -> ConnectionReadyWorker(
                params.trackable,
                params.callbackFunction,
                ably,
                hooks,
                corePublisher,
                params.channelStateChangeListener,
                params.isSubscribedToPresence,
                params.presenceUpdateListener,
            )
            is WorkerParams.RetrySubscribeToPresence -> RetrySubscribeToPresenceWorker(
                params.trackable,
                ably,
                logHandler,
                params.presenceUpdateListener,
            )
            is WorkerParams.RetrySubscribeToPresenceSuccess -> RetrySubscribeToPresenceSuccessWorker(
                params.trackable,
                corePublisher,
            )
            is WorkerParams.DisconnectSuccess -> DisconnectSuccessWorker(
                params.trackable,
                params.callbackFunction,
                corePublisher,
                params.shouldRecalculateResolutionCallback,
                ably,
            )
            is WorkerParams.TrackableRemovalRequested -> TrackableRemovalRequestedWorker(
                params.trackable,
                params.callbackFunction,
                ably,
                params.result,
            )
            is WorkerParams.EnhancedLocationChanged -> EnhancedLocationChangedWorker(
                params.location,
                params.intermediateLocations,
                params.type,
                corePublisher,
                logHandler,
            )
            WorkerParams.RefreshResolutionPolicy -> RefreshResolutionPolicyWorker(
                corePublisher,
            )
            is WorkerParams.Stop -> StopWorker(
                params.callbackFunction,
                ably,
                corePublisher,
                params.timeoutInMilliseconds,
            )
            WorkerParams.StoppingConnectionFinished -> StoppingConnectionFinishedWorker()
        }
    }
}

internal sealed class WorkerParams {
    data class AddTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : WorkerParams()

    data class AddTrackableFailed(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val exception: Exception,
        val isConnectedToAbly: Boolean,
    ) : WorkerParams()

    data class ConnectionCreated(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : WorkerParams()

    data class ConnectionReady(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val isSubscribedToPresence: Boolean,
    ) : WorkerParams()

    data class RetrySubscribeToPresence(
        val trackable: Trackable,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
    ) : WorkerParams()

    data class RetrySubscribeToPresenceSuccess(
        val trackable: Trackable,
    ) : WorkerParams()

    data class DisconnectSuccess(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Unit>,
        val shouldRecalculateResolutionCallback: () -> Unit,
    ) : WorkerParams()

    data class EnhancedLocationChanged(
        val location: Location,
        val intermediateLocations: List<Location>,
        val type: LocationUpdateType,
    ) : WorkerParams()

    object RefreshResolutionPolicy : WorkerParams()

    data class Stop(
        val callbackFunction: ResultCallbackFunction<Unit>,
        val timeoutInMilliseconds: Long,
    ) : WorkerParams()

    data class TrackableRemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>,
    ) : WorkerParams()

    //TODO remove
    object StoppingConnectionFinished : WorkerParams()
}
