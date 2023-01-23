package com.ably.tracking.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConnectionExceptionTests(
    private val statusCode: Int,
    private val isFatal: Boolean,
    private val isRetriable: Boolean,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "Status code: {0} | Is fatal: {1} | Is retriable: {2}"
        )
        fun data() = listOf(
            params(
                statusCode = 399,
                isFatal = false,
                isRetriable = false,
            ),
            params(
                statusCode = 400,
                isFatal = true,
                isRetriable = false,
            ),
            params(
                statusCode = 499,
                isFatal = true,
                isRetriable = false,
            ),
            params(
                statusCode = 500,
                isFatal = false,
                isRetriable = true,
            ),
            params(
                statusCode = 504,
                isFatal = false,
                isRetriable = true,
            ),
            params(
                statusCode = 505,
                isFatal = false,
                isRetriable = false,
            ),
        )

        private fun params(statusCode: Int, isFatal: Boolean, isRetriable: Boolean) =
            arrayOf(statusCode, isFatal, isRetriable)
    }

    @Test
    fun `exception should be treated as fatal if its status code is between 400 and 499`() {
        // given
        val exception = createConnectionException(statusCode)

        // when
        val result = exception.isFatal()

        // then
        Assert.assertEquals(isFatal, result)
    }

    @Test
    fun `exception should be treated as retriable if its status code is between 500 and 504`() {
        // given
        val exception = createConnectionException(statusCode)

        // when
        val result = exception.isRetriable()

        // then
        Assert.assertEquals(isRetriable, result)
    }

    private fun createConnectionException(statusCode: Int): ConnectionException =
        ConnectionException(ErrorInformation(0, statusCode, "Test exception", null, null))
}
