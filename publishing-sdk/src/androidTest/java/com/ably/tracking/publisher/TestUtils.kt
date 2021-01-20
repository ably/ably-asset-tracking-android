package com.ably.tracking.publisher

import com.ably.tracking.Result
import org.junit.Assert
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
open class TestExpectation<T> {
    var result: T? = null

    val semaphore = Semaphore(1).apply {
        this.acquire()
    }

    /**
     * Calls tryAcquire on the encapsulated semaphore.
     * @throws Exception If the timeout was reached before the permit could be acquired.
     */
    fun await(timeoutInSeconds: Long = DEFAULT_ACQUIRE_TIMEOUT_IN_SECONDS) {
        val hasAcquired = semaphore.tryAcquire(1, timeoutInSeconds, TimeUnit.SECONDS)
        if (!hasAcquired) {
            throw Exception("Timeout when attempting to acquire semaphore permit.")
        }
    }

    /**
     * Calls release on the encapsulated semaphore.
     */
    fun fulfill(result: T) {
        this.result = result
        semaphore.release()
    }
}

class UnitResultTestExpectation : TestExpectation<Result<Unit>>() {
    fun assertSuccess() {
        result?.let {
            Assert.assertTrue("Expected success.", it.isSuccess)
            return
        }
        Assert.fail("Expectation unfulfilled.")
    }
}

class UnitTestExpectation : TestExpectation<Unit>()
