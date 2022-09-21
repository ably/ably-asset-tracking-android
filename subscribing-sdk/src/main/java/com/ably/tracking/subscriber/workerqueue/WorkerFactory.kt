package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
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
    private val coreSubscriber: CoreSubscriber,
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
                coreSubscriber,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.SubscribeForPresenceMessages -> SubscribeForPresenceMessagesWorker(
                ably,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.SubscribeToChannel -> SubscribeToChannelWorker(
                coreSubscriber,
                params.callbackFunction
            )
            is WorkerParams.UpdateConnectionState -> UpdateConnectionStateWorker(
                params.connectionStateChange,
                coreSubscriber
            )
            is WorkerParams.UpdateChannelConnectionState -> UpdateChannelConnectionStateWorker(
                params.channelConnectionStateChange,
                coreSubscriber
            )
            is WorkerParams.UpdatePublisherPresence -> UpdatePublisherPresenceWorker(
                params.presenceMessage,
                coreSubscriber
            )
            is WorkerParams.ChangeResolution -> ChangeResolutionWorker(
                ably,
                trackableId,
                params.resolution,
                params.callbackFunction
            )
            is WorkerParams.StopConnection -> StopConnectionWorker(ably, coreSubscriber, params.callbackFunction)
        }
}

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
