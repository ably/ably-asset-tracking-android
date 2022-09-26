package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.workerqueue.workers.ChangeResolutionWorker
import com.ably.tracking.subscriber.workerqueue.workers.SubscribeForPresenceMessagesWorker
import com.ably.tracking.subscriber.workerqueue.workers.SubscribeToChannelWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdatePublisherPresenceWorker
import com.ably.tracking.subscriber.workerqueue.workers.StartConnectionWorker
import com.ably.tracking.subscriber.workerqueue.workers.StopConnectionWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateChannelConnectionStateWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateConnectionStateWorker

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory(
    private val subscriberStateManipulator: SubscriberStateManipulator,
    private val ably: Ably,
    private val trackableId: String
) {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerSpecification].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerSpecification): Worker =
        when (params) {
            is WorkerSpecification.StartConnection -> StartConnectionWorker(
                ably,
                subscriberStateManipulator,
                trackableId,
                params.callbackFunction
            )
            is WorkerSpecification.SubscribeForPresenceMessages -> SubscribeForPresenceMessagesWorker(
                ably,
                trackableId,
                params.callbackFunction
            )
            is WorkerSpecification.SubscribeToChannel -> SubscribeToChannelWorker(
                subscriberStateManipulator,
                params.callbackFunction
            )
            is WorkerSpecification.UpdateConnectionState -> UpdateConnectionStateWorker(
                params.connectionStateChange,
                subscriberStateManipulator
            )
            is WorkerSpecification.UpdateChannelConnectionState -> UpdateChannelConnectionStateWorker(
                params.channelConnectionStateChange,
                subscriberStateManipulator
            )
            is WorkerSpecification.UpdatePublisherPresence -> UpdatePublisherPresenceWorker(
                params.presenceMessage,
                subscriberStateManipulator
            )
            is WorkerSpecification.ChangeResolution -> ChangeResolutionWorker(
                ably,
                trackableId,
                params.resolution,
                params.callbackFunction
            )
            is WorkerSpecification.StopConnection -> StopConnectionWorker(ably, subscriberStateManipulator, params.callbackFunction)
        }
}

internal sealed class WorkerSpecification {
    data class UpdateConnectionState(
        val connectionStateChange: ConnectionStateChange
    ) : WorkerSpecification()

    data class UpdateChannelConnectionState(
        val channelConnectionStateChange: ConnectionStateChange
    ) : WorkerSpecification()

    data class UpdatePublisherPresence(
        val presenceMessage: PresenceMessage
    ) : WorkerSpecification()

    data class ChangeResolution(
        val resolution: Resolution?,
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class StartConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class SubscribeForPresenceMessages(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class SubscribeToChannel(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()

    data class StopConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerSpecification()
}
