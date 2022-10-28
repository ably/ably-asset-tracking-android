package com.ably.tracking.common.workerqueue

/**
 * A worker factory is responsible for instantiating [Worker] from passed specification.
 * Params:
 * Properties - the type of properties used by created workers
 * WorkerSpecification - the type of specification used to create workers
 */
interface WorkerFactory<Properties : QueueProperties, WorkerSpecification> {

    /**
     * Creates an appropriate [Worker] from the passed specification [WorkerSpecification].
     *
     * @param params The parameters that indicate which [Worker] implementation should be created.
     * @return New [Worker] instance.
     */
    fun createWorker(params: WorkerSpecification): Worker<Properties, WorkerSpecification>
}
