package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.workerqueue.workers.HandleConnectionReadyWorker
import com.ably.tracking.subscriber.workerqueue.workers.HandlePresenceMessageWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateChannelConnectionStateWorker
import com.ably.tracking.subscriber.workerqueue.workers.UpdateConnectionStateWorker

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory(private val coreSubscriber: CoreSubscriber) {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerParams].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerParams): Worker =
        when (params) {
            is WorkerParams.UpdateConnectionState -> UpdateConnectionStateWorker(
                params.connectionStateChange,
                coreSubscriber
            )
            is WorkerParams.UpdateChannelConnectionState -> UpdateChannelConnectionStateWorker(
                params.channelConnectionStateChange,
                coreSubscriber
            )
            is WorkerParams.HandlePresenceMessage -> HandlePresenceMessageWorker(
                params.presenceMessage,
                coreSubscriber
            )
            is WorkerParams.HandleConnectionReady -> HandleConnectionReadyWorker(
                coreSubscriber,
                params.callbackFunction
            )
        }
}


internal sealed class WorkerParams {
    data class UpdateConnectionState(
        val connectionStateChange: ConnectionStateChange
    ) : WorkerParams()

    data class UpdateChannelConnectionState(
        val channelConnectionStateChange: ConnectionStateChange
    ) : WorkerParams()

    data class HandlePresenceMessage(
        val presenceMessage: PresenceMessage
    ) : WorkerParams()

    data class HandleConnectionReady(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

}
