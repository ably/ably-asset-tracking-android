package com.ably.tracking.common.workerqueue

/**
 * An abstract base class, containing properties that affect [WorkerQueue] behavior.
 */
abstract class Properties {
    abstract val isStopped: Boolean
}
