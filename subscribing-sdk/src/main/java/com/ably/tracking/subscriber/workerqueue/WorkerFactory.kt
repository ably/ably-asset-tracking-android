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
     * Creates an appropriate [Worker] from the passed [WorkerParams].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerParams): Worker =
        when (params) {
            is WorkerParams.StartConnection -> StartConnectionWorker(
                ably,
                subscriberStateManipulator,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.SubscribeForPresenceMessages -> SubscribeForPresenceMessagesWorker(
                ably,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.SubscribeToChannel -> SubscribeToChannelWorker(
                subscriberStateManipulator,
                params.callbackFunction
            )
            is WorkerParams.UpdateConnectionState -> UpdateConnectionStateWorker(
                params.connectionStateChange,
                subscriberStateManipulator
            )
            is WorkerParams.UpdateChannelConnectionState -> UpdateChannelConnectionStateWorker(
                params.channelConnectionStateChange,
                subscriberStateManipulator
            )
            is WorkerParams.UpdatePublisherPresence -> UpdatePublisherPresenceWorker(
                params.presenceMessage,
                subscriberStateManipulator
            )
            is WorkerParams.ChangeResolution -> ChangeResolutionWorker(
                ably,
                trackableId,
                params.resolution,
                params.callbackFunction
            )
            is WorkerParams.StopConnection -> StopConnectionWorker(ably, subscriberStateManipulator, params.callbackFunction)
        }
}

//TODO rename to WorkerSpecification
internal sealed class WorkerParams {
    data class UpdateConnectionState(
        val connectionStateChange: ConnectionStateChange
    ) : WorkerParams()

    data class UpdateChannelConnectionState(
        val channelConnectionStateChange: ConnectionStateChange
    ) : WorkerParams()

    data class UpdatePublisherPresence(
        val presenceMessage: PresenceMessage
    ) : WorkerParams()

    data class ChangeResolution(
        val resolution: Resolution?,
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class StartConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class SubscribeForPresenceMessages(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class SubscribeToChannel(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class StopConnection(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()
}
