package com.ably.tracking.common.workerqueue

/**
 * A worker factory is responsible for instantiating [Worker] from passed specification.
 * Params:
 * P - the type of properties used by created workers
 * S - the type of specification used to create workers
 */
interface WorkerFactory<P : Properties, S> {

    /**
     * Creates an appropriate [Worker] from the passed specification [S].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: S): Worker<P, S>
}
