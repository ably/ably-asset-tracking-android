package com.ably.tracking.subscriber.workerqueue

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.CoreSubscriber
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
        }
}


internal sealed class WorkerParams {
    data class UpdateConnectionState(
        val connectionStateChange: ConnectionStateChange
    ) : WorkerParams()
}
