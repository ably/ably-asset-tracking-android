package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.workerqueue.workers.ChangeResolutionWorker
import com.ably.tracking.subscriber.workerqueue.workers.HandleConnectionCreatedWorker
import com.ably.tracking.subscriber.workerqueue.workers.HandleConnectionReadyWorker
import com.ably.tracking.subscriber.workerqueue.workers.HandlePresenceMessageWorker
import com.ably.tracking.subscriber.workerqueue.workers.StartWorker
import com.ably.tracking.subscriber.workerqueue.workers.StopWorker
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
            is WorkerParams.Start -> StartWorker(
                ably,
                coreSubscriber,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.HandleConnectionCreated -> HandleConnectionCreatedWorker(
                ably,
                trackableId,
                params.callbackFunction
            )
            is WorkerParams.HandleConnectionReady -> HandleConnectionReadyWorker(
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
            is WorkerParams.HandlePresenceMessage -> HandlePresenceMessageWorker(
                params.presenceMessage,
                coreSubscriber
            )
            is WorkerParams.ChangeResolution -> ChangeResolutionWorker(
                ably,
                trackableId,
                params.resolution,
                params.callbackFunction
            )
            is WorkerParams.Stop -> StopWorker(ably, coreSubscriber, params.callbackFunction)
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

    data class ChangeResolution(
        val resolution: Resolution?,
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class Start(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class HandleConnectionCreated(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class HandleConnectionReady(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

    data class Stop(
        val callbackFunction: ResultCallbackFunction<Unit>
    ) : WorkerParams()

}
