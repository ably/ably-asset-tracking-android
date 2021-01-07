package com.ably.tracking.publisher

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * A timeout appropriate for most integration tests.
 */
private const val DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS = 5L

/**
 * Encapsulates a semaphore with a single permit, acquired from the outset.
 */
class TestLock {
    val semaphore = Semaphore(1).apply { this.acquire() }

    /**
     * Calls tryAcquire on the encapsulated semaphore.
     * @throws Exception If the timeout was reached before the permit could be acquired.
     */
    fun acquire(timeoutInSeconds: Long = DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS) {
        val hasAcquired = semaphore.tryAcquire(1, timeoutInSeconds, TimeUnit.SECONDS)
        if (!hasAcquired) {
            throw Exception("Timeout when attempting to acquire semaphore permit.")
        }
    }

    /**
     * Calls release on the encapsulated semaphore.
     */
    fun release() {
        semaphore.release()
    }
}
