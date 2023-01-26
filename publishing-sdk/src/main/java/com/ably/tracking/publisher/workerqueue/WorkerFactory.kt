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
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.common.workerqueue.WorkerFactory
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.Mapbox
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
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
import com.ably.tracking.publisher.workerqueue.workers.RetrySubscribeToPresenceSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.RetrySubscribeToPresenceWorker
import com.ably.tracking.publisher.workerqueue.workers.SendEnhancedLocationFailureWorker
import com.ably.tracking.publisher.workerqueue.workers.SendEnhancedLocationSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.SendRawLocationFailureWorker
import com.ably.tracking.publisher.workerqueue.workers.SendRawLocationSuccessWorker
import com.ably.tracking.publisher.workerqueue.workers.SetActiveTrackableWorker
import com.ably.tracking.publisher.workerqueue.workers.StopWorker
import com.ably.tracking.publisher.workerqueue.workers.StoppingConnectionFinishedWorker
import com.ably.tracking.publisher.workerqueue.workers.TrackableRemovalRequestedWorker
import kotlinx.coroutines.flow.StateFlow

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory(
    private val ably: Ably,
    private val hooks: DefaultCorePublisher.Hooks,
    private val publisherInteractor: PublisherInteractor,
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
            is WorkerSpecification.AddTrackable -> AddTrackableWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                workerSpecification.presenceUpdateListener,
                workerSpecification.channelStateChangeListener,
                ably,
            )
            is WorkerSpecification.AddTrackableFailed -> AddTrackableFailedWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                workerSpecification.exception,
                workerSpecification.isConnectedToAbly,
                ably,
            )
            is WorkerSpecification.ConnectionCreated -> ConnectionCreatedWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                ably,
                logHandler,
                workerSpecification.presenceUpdateListener,
                workerSpecification.channelStateChangeListener,
            )
            is WorkerSpecification.ConnectionReady -> ConnectionReadyWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                ably,
                hooks,
                publisherInteractor,
                workerSpecification.channelStateChangeListener,
                workerSpecification.isSubscribedToPresence,
                workerSpecification.presenceUpdateListener,
            )
            is WorkerSpecification.RetrySubscribeToPresence -> RetrySubscribeToPresenceWorker(
                workerSpecification.trackable,
                ably,
                logHandler,
                workerSpecification.presenceUpdateListener,
            )
            is WorkerSpecification.RetrySubscribeToPresenceSuccess -> RetrySubscribeToPresenceSuccessWorker(
                workerSpecification.trackable,
                publisherInteractor,
            )
            is WorkerSpecification.DisconnectSuccess -> DisconnectSuccessWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                publisherInteractor,
                workerSpecification.shouldRecalculateResolutionCallback,
                ably,
            )
            is WorkerSpecification.TrackableRemovalRequested -> TrackableRemovalRequestedWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                ably,
                workerSpecification.result,
            )
            is WorkerSpecification.AblyConnectionStateChange -> AblyConnectionStateChangeWorker(
                workerSpecification.connectionStateChange,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.ChangeLocationEngineResolution -> ChangeLocationEngineResolutionWorker(
                resolutionPolicy,
                mapbox,
            )
            is WorkerSpecification.ChangeRoutingProfile -> ChangeRoutingProfileWorker(
                workerSpecification.routingProfile,
                publisherInteractor,
            )
            is WorkerSpecification.ChannelConnectionStateChange -> ChannelConnectionStateChangeWorker(
                workerSpecification.trackableId,
                workerSpecification.connectionStateChange,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.DestinationSet -> DestinationSetWorker(
                workerSpecification.routeDurationInMilliseconds,
                timeProvider,
            )
            is WorkerSpecification.EnhancedLocationChanged -> EnhancedLocationChangedWorker(
                workerSpecification.location,
                workerSpecification.intermediateLocations,
                workerSpecification.type,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.PresenceMessage -> PresenceMessageWorker(
                workerSpecification.trackable,
                workerSpecification.presenceMessage,
                publisherInteractor,
            )
            is WorkerSpecification.RawLocationChanged -> RawLocationChangedWorker(
                workerSpecification.location,
                publisherInteractor,
                logHandler,
            )
            WorkerSpecification.RefreshResolutionPolicy -> RefreshResolutionPolicyWorker(
                publisherInteractor,
            )
            is WorkerSpecification.RemoveTrackable -> RemoveTrackableWorker(
                workerSpecification.trackable,
                ably,
                publisherInteractor,
                workerSpecification.callbackFunction,
            )
            is WorkerSpecification.SendEnhancedLocationFailure -> SendEnhancedLocationFailureWorker(
                workerSpecification.locationUpdate,
                workerSpecification.trackableId,
                workerSpecification.exception,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.SendEnhancedLocationSuccess -> SendEnhancedLocationSuccessWorker(
                workerSpecification.location,
                workerSpecification.trackableId,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.SendRawLocationFailure -> SendRawLocationFailureWorker(
                workerSpecification.locationUpdate,
                workerSpecification.trackableId,
                workerSpecification.exception,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.SendRawLocationSuccess -> SendRawLocationSuccessWorker(
                workerSpecification.location,
                workerSpecification.trackableId,
                publisherInteractor,
                logHandler,
            )
            is WorkerSpecification.SetActiveTrackable -> SetActiveTrackableWorker(
                workerSpecification.trackable,
                workerSpecification.callbackFunction,
                publisherInteractor,
                hooks,
            )
            is WorkerSpecification.Stop -> StopWorker(
                workerSpecification.callbackFunction,
                ably,
                publisherInteractor,
            )
            WorkerSpecification.StoppingConnectionFinished -> StoppingConnectionFinishedWorker()
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
    ) : WorkerSpecification()

    data class TrackableRemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>,
    ) : WorkerSpecification()

    object StoppingConnectionFinished : WorkerSpecification()
}
