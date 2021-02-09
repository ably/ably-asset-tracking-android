package com.ably.tracking.test.common

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * A timeout appropriate for most integration tests.
 */
private const val DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS = 5L

/**
 * Encapsulates a semaphore with a single permit, acquired from the outset.
 *
 * When the expectation is fulfilled the permit is released, allowing await to succeed.
 */
open class Expectation<T>(
    /**
     * A description of what we're waiting for.
     */
    val description: String
) {
    private var waiting: Boolean = false
    var result: T? = null

    private val semaphore = Semaphore(1).apply {
        this.acquire()
    }

    /**
     * Calls tryAcquire on the encapsulated semaphore.
     */
    fun await(timeoutInSeconds: Long = DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS) {
        if (waiting) {
            throw AssertionError("Already awaiting expectation '$description'.")
        }
        waiting = true
        testLogD("semaphore '$description' acquire...")
        val acquired = semaphore.tryAcquire(1, timeoutInSeconds, TimeUnit.SECONDS)
        testLogD("semaphore '$description' ${if (acquired) "acquired" else "failed to acquire"}")
        waiting = false
    }

    /**
     * Calls release on the encapsulated semaphore.
     */
    fun fulfill(result: T) {
        if (null != this.result) {
            throw AssertionError("Expectation '$description' already fulfilled.")
        }
        this.result = result
        semaphore.release()
    }

    fun assertFulfilled(): T =
        result ?: throw AssertionError("Expectation '$description' unfulfilled.")
}

class UnitResultExpectation(label: String) : Expectation<Result<Unit>>(label) {
    fun assertSuccess() {
        assertFulfilled().let {
            if (!it.isSuccess) {
                throw AssertionError("Expectation '$description' did not result in success.")
            }
        }
    }
}

class UnitExpectation(label: String) : Expectation<Unit>(label) {
    fun fulfill() {
        fulfill(Unit)
    }
}
