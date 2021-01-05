package com.ably.tracking.common

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private const val DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS = 5L

class TestLock {
    val semaphore = Semaphore(1).apply { this.acquire() }

    fun acquire(timeoutInSeconds: Long = DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS) {
        val hasAcquired = semaphore.tryAcquire(1, timeoutInSeconds, TimeUnit.SECONDS)
        if (!hasAcquired) {
            throw Exception("TestLock Timeout")
        }
    }

    fun release() {
        semaphore.release()
    }
}
