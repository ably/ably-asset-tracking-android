package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.common.workerqueue.WorkerFactory
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.workerqueue.workers.ChangeResolutionSuccessWorker
import com.ably.tracking.subscriber.workerqueue.workers.ChangeResolutionWorker
import com.ably.tracking.subscriber.workerqueue.workers.DeprecatedChangeResolutionWorker
import com.ably.tracking.subscriber.workerqueue.workers.DisconnectWorker
import com.ably.tracking.subscriber.workerqueue.workers.FetchHistoryForChannelConnectionStateChangeWorker
import com.ably.tracking.subscriber.workerqueue.workers.EnterPresenceWorker
import com.ably.tracking.subscriber.workerqueue.workers.ProcessInitialPresenceMessagesWorker
import com.ably.tracking.subscriber.workerqueue.workers.StartConnectionWorker
import com.ably.tracking.subscriber.workerqueue.workers.StopConnectionWorker
import com.ably.tracking.subscriber.workerqueue.workers.SubscribeForPresenceMessagesWorker
import com.ably.tracking.subscriber.workerqueue.workers.SubscribeToChannelWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateChannelConnectionStateWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateConnectionStateWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdatePublisherPresenceWorker

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory(
    private val subscriberInteractor: SubscriberInteractor,
    private val ably: Ably,
    private val trackableId: String
) : WorkerFactory<SubscriberProperties, WorkerSpecification> {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerSpecification].
     *
     * @param workerSpecification The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    override fun createWorker(workerSpecification: WorkerSpecification): Worker<SubscriberProperties, WorkerSpecification> =
        when (workerSpecification) {
            is WorkerSpecification.StartConnection -> StartConnectionWorker(
                ably,
                trackableId,
                workerSpecification.callbackFunction
            )
            is WorkerSpecification.EnterPresence -> EnterPresenceWorker(
                workerSpecification.trackableId,
                ably
            )
            is WorkerSpecification.SubscribeForPresenceMessages -> SubscribeForPresenceMessagesWorker(
                ably,
                trackableId
            )
            is WorkerSpecification.SubscribeToChannel -> SubscribeToChannelWorker(
                subscriberInteractor
            )
            is WorkerSpecification.UpdateConnectionState -> UpdateConnectionStateWorker(
                workerSpecification.connectionStateChange
            )
            is WorkerSpecification.UpdateChannelConnectionState -> UpdateChannelConnectionStateWorker(
                workerSpecification.channelConnectionStateChange,
                workerSpecification.presenceHistory
            )
            is WorkerSpecification.FetchHistoryForChannelConnectionStateChange -> FetchHistoryForChannelConnectionStateChangeWorker(
                workerSpecification.trackableId,
                workerSpecification.channelConnectionStateChange,
                ably
            )
            is WorkerSpecification.UpdatePublisherPresence -> UpdatePublisherPresenceWorker(
                workerSpecification.presenceMessage
            )
            is WorkerSpecification.DeprecatedChangeResolution -> DeprecatedChangeResolutionWorker(
                ably,
                trackableId,
                workerSpecification.resolution,
                workerSpecification.callbackFunction
            )
            is WorkerSpecification.ChangeResolution -> ChangeResolutionWorker(
                ably,
                trackableId,
                workerSpecification.resolution
            )
            is WorkerSpecification.ChangeResolutionSuccess -> ChangeResolutionSuccessWorker(
                trackableId,
                workerSpecification.resolution
            )
            is WorkerSpecification.Disconnect -> DisconnectWorker(
                ably,
                workerSpecification.trackableId,
                workerSpecification.callbackFunction
            )
            is WorkerSpecification.StopConnection -> StopConnectionWorker(
                ably,
                subscriberInteractor,
                workerSpecification.callbackFunction
            )
            is WorkerSpecification.ProcessInitialPresenceMessages -> ProcessInitialPresenceMessagesWorker(
                workerSpecification.presenceMessages
            )
        }
}

internal sealed class WorkerSpecification {
    data class UpdateConnectionState(
        val connectionStateChange: ConnectionStateChange
    ) : WorkerSpecification()

    data class UpdateChannelConnectionState(
        val channelConnectionStateChange: ConnectionStateChange,
        val presenceHistory: List<PresenceMessage>?
    ) : WorkerSpecification()

    data class FetchHistoryForChannelConnectionStateChange(
        val trackableId: String,
        val channelConnectionStateChange: ConnectionStateChange
    ) : WorkerSpecification()

    data class UpdatePublisherPresence(
        val presenceMessage: PresenceMessage
    ) : WorkerSpecification()

    data class DeprecatedChangeResolution(
        val resolution: Resolution?,
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class ChangeResolution(
        val resolution: Resolution?
    ) : WorkerSpecification()

    data class ChangeResolutionSuccess(
        val resolution: Resolution?
    ) : WorkerSpecification()

    data class StartConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class EnterPresence(val trackableId: String) : WorkerSpecification()

    object SubscribeForPresenceMessages : WorkerSpecification()

    object SubscribeToChannel : WorkerSpecification()

    data class Disconnect(
        val trackableId: String,
        val callbackFunction: () -> Unit
    ) : WorkerSpecification()

    data class StopConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class ProcessInitialPresenceMessages(val presenceMessages: List<PresenceMessage>) :
        WorkerSpecification()
}
