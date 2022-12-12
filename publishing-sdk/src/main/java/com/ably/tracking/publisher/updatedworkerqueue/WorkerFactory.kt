package com.ably.tracking.publisher.updatedworkerqueue

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.TimeProvider
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.common.workerqueue.WorkerFactory
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.ResolutionPolicy
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.updatedworkerqueue.workers.AblyConnectionStateChangeWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.ChangeLocationEngineResolutionWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.RawLocationChangedWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.SendRawLocationFailureWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.SendRawLocationSuccessWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.SetActiveTrackableWorker
import com.ably.tracking.publisher.updatedworkerqueue.workers.StoppingConnectionFinishedWorker
import kotlinx.coroutines.flow.StateFlow

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory(
    private val ably: Ably,
    private val hooks: DefaultCorePublisher.Hooks,
    private val corePublisher: CorePublisher,
    private val resolutionPolicy: ResolutionPolicy,
    private val mapbox: Mapbox,
    private val timeProvider: TimeProvider,
    private val logHandler: LogHandler?,
) :
    WorkerFactory<PublisherProperties, WorkerSpecification> {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerSpecification].
     *
     * @param workerSpecification The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    override fun createWorker(workerSpecification: WorkerSpecification): Worker<PublisherProperties, WorkerSpecification> =
        when (workerSpecification) {
//            is WorkerParams.AddTrackable -> AddTrackableWorker(
//                params.trackable,
//                params.callbackFunction,
//                params.presenceUpdateListener,
//                params.channelStateChangeListener,
//                ably,
//            )
//            is WorkerParams.AddTrackableFailed -> AddTrackableFailedWorker(
//                params.trackable,
//                params.callbackFunction,
//                params.exception,
//                params.isConnectedToAbly,
//                ably,
//            )
//            is WorkerParams.ConnectionCreated -> ConnectionCreatedWorker(
//                params.trackable,
//                params.callbackFunction,
//                ably,
//                logHandler,
//                params.presenceUpdateListener,
//                params.channelStateChangeListener,
//            )
//            is WorkerParams.ConnectionReady -> ConnectionReadyWorker(
//                params.trackable,
//                params.callbackFunction,
//                ably,
//                hooks,
//                corePublisher,
//                params.channelStateChangeListener,
//                params.isSubscribedToPresence,
//                params.presenceUpdateListener,
//            )
//            is WorkerParams.RetrySubscribeToPresence -> RetrySubscribeToPresenceWorker(
//                params.trackable,
//                ably,
//                logHandler,
//                params.presenceUpdateListener,
//            )
//            is WorkerParams.RetrySubscribeToPresenceSuccess -> RetrySubscribeToPresenceSuccessWorker(
//                params.trackable,
//                corePublisher,
//            )
//            is WorkerParams.DisconnectSuccess -> DisconnectSuccessWorker(
//                params.trackable,
//                params.callbackFunction,
//                corePublisher,
//                params.shouldRecalculateResolutionCallback,
//                ably,
//            )
//            is WorkerParams.TrackableRemovalRequested -> TrackableRemovalRequestedWorker(
//                params.trackable,
//                params.callbackFunction,
//                ably,
//                params.result,
//            )
            is WorkerSpecification.AblyConnectionStateChange -> AblyConnectionStateChangeWorker(
                workerSpecification.connectionStateChange,
                corePublisher,
                logHandler,
            )
            is WorkerSpecification.ChangeLocationEngineResolution -> ChangeLocationEngineResolutionWorker(
                resolutionPolicy,
                mapbox,
            )
//            is WorkerParams.ChangeRoutingProfile -> ChangeRoutingProfileWorker(
//                params.routingProfile,
//                corePublisher,
//            )
            is WorkerSpecification.ChannelConnectionStateChange -> com.ably.tracking.publisher.updatedworkerqueue.workers.ChannelConnectionStateChangeWorker(
                workerSpecification.trackableId,
                workerSpecification.connectionStateChange,
                corePublisher,
                logHandler,
            )
//            is WorkerParams.DestinationSet -> DestinationSetWorker(
//                params.routeDurationInMilliseconds,
//                timeProvider,
//            )
//            is WorkerParams.EnhancedLocationChanged -> EnhancedLocationChangedWorker(
//                params.location,
//                params.intermediateLocations,
//                params.type,
//                corePublisher,
//                logHandler,
//            )
//            is WorkerParams.PresenceMessage -> PresenceMessageWorker(
//                params.trackable,
//                params.presenceMessage,
//                corePublisher,
//            )
            is WorkerSpecification.RawLocationChanged -> RawLocationChangedWorker(
                workerSpecification.location,
                corePublisher,
                logHandler,
            )
//            WorkerParams.RefreshResolutionPolicy -> RefreshResolutionPolicyWorker(
//                corePublisher,
//            )
//            is WorkerParams.RemoveTrackable -> RemoveTrackableWorker(
//                params.trackable,
//                params.callbackFunction,
//                ably,
//            )
//            is WorkerParams.SendEnhancedLocationFailure -> SendEnhancedLocationFailureWorker(
//                params.locationUpdate,
//                params.trackableId,
//                params.exception,
//                corePublisher,
//                logHandler,
//            )
//            is WorkerParams.SendEnhancedLocationSuccess -> SendEnhancedLocationSuccessWorker(
//                params.location,
//                params.trackableId,
//                corePublisher,
//                logHandler,
//            )
            is WorkerSpecification.SendRawLocationFailure -> SendRawLocationFailureWorker(
                workerSpecification.locationUpdate,
                workerSpecification.trackableId,
                workerSpecification.exception,
                corePublisher,
                logHandler,
            )
            is WorkerSpecification.SendRawLocationSuccess -> SendRawLocationSuccessWorker(
                workerSpecification.location,
                workerSpecification.trackableId,
                corePublisher,
                logHandler,
            )
            is WorkerSpecification.SetActiveTrackable -> SetActiveTrackableWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                corePublisher,
                hooks,
            )
//            is WorkerParams.Stop -> StopWorker(
//                params.callbackFunction,
//                ably,
//                corePublisher,
//                params.timeoutInMilliseconds,
//            )
            WorkerSpecification.StoppingConnectionFinished -> StoppingConnectionFinishedWorker()
            else -> throw NotImplementedError()
        }
}

internal sealed class WorkerSpecification {
    data class AblyConnectionStateChange(
        val connectionStateChange: ConnectionStateChange,
    ) : WorkerSpecification()

    data class AddTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : WorkerSpecification()

    data class AddTrackableFailed(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val exception: Exception,
        val isConnectedToAbly: Boolean,
    ) : WorkerSpecification()

    object ChangeLocationEngineResolution : WorkerSpecification()

    data class ChangeRoutingProfile(
        val routingProfile: RoutingProfile,
    ) : WorkerSpecification()

    data class ChannelConnectionStateChange(
        val trackableId: String,
        val connectionStateChange: ConnectionStateChange,
    ) : WorkerSpecification()

    data class ConnectionCreated(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : WorkerSpecification()

    data class ConnectionReady(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
        val isSubscribedToPresence: Boolean,
    ) : WorkerSpecification()

    data class RetrySubscribeToPresence(
        val trackable: Trackable,
        val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
    ) : WorkerSpecification()

    data class RetrySubscribeToPresenceSuccess(
        val trackable: Trackable,
    ) : WorkerSpecification()

    data class DestinationSet(
        val routeDurationInMilliseconds: Long,
    ) : WorkerSpecification()

    data class DisconnectSuccess(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Unit>,
        val shouldRecalculateResolutionCallback: () -> Unit,
    ) : WorkerSpecification()

    data class EnhancedLocationChanged(
        val location: Location,
        val intermediateLocations: List<Location>,
        val type: LocationUpdateType,
    ) : WorkerSpecification()

    data class PresenceMessage(
        val trackable: Trackable,
        val presenceMessage: com.ably.tracking.common.PresenceMessage,
    ) : WorkerSpecification()

    data class RawLocationChanged(
        val location: Location,
    ) : WorkerSpecification()

    object RefreshResolutionPolicy : WorkerSpecification()

    data class RemoveTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Boolean>,
    ) : WorkerSpecification()

    data class SendEnhancedLocationFailure(
        val locationUpdate: EnhancedLocationUpdate,
        val trackableId: String,
        val exception: Throwable?,
    ) : WorkerSpecification()

    data class SendEnhancedLocationSuccess(
        val location: Location,
        val trackableId: String,
    ) : WorkerSpecification()

    data class SendRawLocationFailure(
        val locationUpdate: LocationUpdate,
        val trackableId: String,
        val exception: Throwable?,
    ) : WorkerSpecification()

    data class SendRawLocationSuccess(
        val location: Location,
        val trackableId: String,
    ) : WorkerSpecification()

    data class SetActiveTrackable(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<Unit>,
    ) : WorkerSpecification()

    data class Stop(
        val callbackFunction: ResultCallbackFunction<Unit>,
        val timeoutInMilliseconds: Long,
    ) : WorkerSpecification()

    data class TrackableRemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>,
    ) : WorkerSpecification()

    object StoppingConnectionFinished : WorkerSpecification()
}
