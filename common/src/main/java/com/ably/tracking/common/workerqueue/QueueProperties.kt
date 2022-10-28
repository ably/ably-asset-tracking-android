package com.ably.tracking.common.workerqueue

/**
 * An interface containing properties that affect [WorkerQueue] behavior.
 */
interface QueueProperties {
    val isStopped: Boolean
}
