package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
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
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.workers.AblyConnectionStateChangeWorker
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.ChangeLocationEngineResolutionWorker
import com.ably.tracking.publisher.workerqueue.workers.ChangeRoutingProfileWorker
import com.ably.tracking.publisher.workerqueue.workers.ChannelConnectionStateChangeWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionCreatedWorker
import com.ably.tracking.publisher.workerqueue.workers.ConnectionReadyWorker
import com.ably.tracking.publisher.workerqueue.workers.DestinationSetWorker
import com.ably.tracking.publisher.workerqueue.workers.DisconnectSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.EnhancedLocationChangedWorker
import com.ably.tracking.publisher.workerqueue.workers.PresenceMessageWorker
import com.ably.tracking.publisher.workerqueue.workers.RawLocationChangedWorker
import com.ably.tracking.publisher.workerqueue.workers.RefreshResolutionPolicyWorker
import com.ably.tracking.publisher.workerqueue.workers.RemoveTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.SendEnhancedLocationFailureWorker
import com.ably.tracking.publisher.workerqueue.workers.SendEnhancedLocationSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.SendRawLocationFailureWorker
import com.ably.tracking.publisher.workerqueue.workers.SendRawLocationSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.SetActiveTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.StopWorker
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
            )
            is WorkerParams.ConnectionCreated -> ConnectionCreatedWorker(
                params.trackable,
                params.callbackFunction,
                ably,
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
            )
            is WorkerParams.DisconnectSuccess -> DisconnectSuccessWorker(
                params.trackable,
                params.callbackFunction,
                corePublisher,
                params.shouldRecalculateResolutionCallback,
            )
            is WorkerParams.TrackableRemovalRequested -> TrackableRemovalRequestedWorker(
                params.trackable,
                params.callbackFunction,
                params.result,
            )
            is WorkerParams.AblyConnectionStateChange -> AblyConnectionStateChangeWorker(
                params.connectionStateChange,
                corePublisher,
                logHandler,
            )
            WorkerParams.ChangeLocationEngineResolution -> ChangeLocationEngineResolutionWorker(
                resolutionPolicy,
                mapbox,
            )
            is WorkerParams.ChangeRoutingProfile -> ChangeRoutingProfileWorker(
                params.routingProfile,
                corePublisher,
            )
            is WorkerParams.ChannelConnectionStateChange -> ChannelConnectionStateChangeWorker(
                params.connectionStateChange,
                params.trackableId,
                corePublisher,
                logHandler,
            )
            is WorkerParams.DestinationSet -> DestinationSetWorker(
                params.routeDurationInMilliseconds,
                timeProvider,
            )
            is WorkerParams.EnhancedLocationChanged -> EnhancedLocationChangedWorker(
                params.location,
                params.intermediateLocations,
                params.type,
                corePublisher,
                logHandler,
            )
            is WorkerParams.PresenceMessage -> PresenceMessageWorker(
                params.trackable,
                params.presenceMessage,
                corePublisher,
            )
            is WorkerParams.RawLocationChanged -> RawLocationChangedWorker(
                params.location,
                corePublisher,
                logHandler,
            )
            WorkerParams.RefreshResolutionPolicy -> RefreshResolutionPolicyWorker(
                corePublisher,
            )
            is WorkerParams.RemoveTrackable -> RemoveTrackableWorker(
                params.trackable,
                params.callbackFunction,
                ably,
            )
            is WorkerParams.SendEnhancedLocationFailure -> SendEnhancedLocationFailureWorker(
                params.locationUpdate,
                params.trackableId,
                params.exception,
                corePublisher,
                logHandler,
            )
            is WorkerParams.SendEnhancedLocationSuccess -> SendEnhancedLocationSuccessWorker(
                params.location,
                params.trackableId,
                corePublisher,
                logHandler,
            )
            is WorkerParams.SendRawLocationFailure -> SendRawLocationFailureWorker(
                params.locationUpdate,
                params.trackableId,
                params.exception,
                corePublisher,
                logHandler,
            )
            is WorkerParams.SendRawLocationSuccess -> SendRawLocationSuccessWorker(
                params.location,
                params.trackableId,
                corePublisher,
                logHandler,
            )
            is WorkerParams.SetActiveTrackable -> SetActiveTrackableWorker(
                params.trackable,
                params.callbackFunction,
                corePublisher,
                hooks,
            )
            is WorkerParams.Stop -> StopWorker(
                params.callbackFunction,
                ably,
                corePublisher,
                params.timeoutInMilliseconds,
            )
        }
    }
}

internal sealed class WorkerParams {
    data class AblyConnectionStateChange(
        val connectionStateChange: ConnectionStateChange,
    ) : WorkerParams()

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
    ) : WorkerParams()

    object ChangeLocationEngineResolution : WorkerParams()

    data class ChangeRoutingProfile(
        val routingProfile: RoutingProfile,
    ) : WorkerParams()

    data class ChannelConnectionStateChange(
        val trackableId: String,
        val connectionStateChange: ConnectionStateChange,
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
    ) : WorkerParams()

    data class DestinationSet(
        val routeDurationInMilliseconds: Long,
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

    data class PresenceMessage(
        val trackable: Trackable,
        val presenceMessage: com.ably.tracking.common.PresenceMessage,
    ) : WorkerParams()

    data class RawLocationChanged(
        val location: Location,
    ) : WorkerParams()

    object RefreshResolutionPolicy : WorkerParams()

    data class RemoveTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Boolean>,
    ) : WorkerParams()

    data class SendEnhancedLocationFailure(
        val locationUpdate: EnhancedLocationUpdate,
        val trackableId: String,
        val exception: Throwable?,
    ) : WorkerParams()

    data class SendEnhancedLocationSuccess(
        val location: Location,
        val trackableId: String,
    ) : WorkerParams()

    data class SendRawLocationFailure(
        val locationUpdate: LocationUpdate,
        val trackableId: String,
        val exception: Throwable?,
    ) : WorkerParams()

    data class SendRawLocationSuccess(
        val location: Location,
        val trackableId: String,
    ) : WorkerParams()

    data class SetActiveTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Unit>,
    ) : WorkerParams()

    data class Stop(
        val callbackFunction: ResultCallbackFunction<Unit>,
        val timeoutInMilliseconds: Long,
    ) : WorkerParams()

    data class TrackableRemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>,
    ) : WorkerParams()
}
