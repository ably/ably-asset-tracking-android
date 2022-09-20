package com.ably.tracking.subscriber.workerqueue

/**
 * Factory that creates the [Worker]s. It also serves as a simple DI for workers dependencies.
 */
internal class WorkerFactory {
    /**
     * Creates an appropriate [Worker] from the passed [WorkerParams].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerParams): Worker{
        throw NotImplementedError()
    }
}


internal sealed class WorkerParams
