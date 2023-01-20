package com.ably.tracking.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import org.junit.Assert
import org.junit.Test

class ConnectionExceptionTests {
    @Test
    fun `exception with error code starting with 4 should be treated as fatal`() {
        // given
        val exception = createConnectionException(400, 40000, "Fatal error")

        // when
        val result = exception.isFatal()

        // then
        Assert.assertTrue(result)
    }

    @Test
    fun `exception with error code starting with 4 should not be treated as retriable`() {
        // given
        val exception = createConnectionException(400, 40000, "Fatal error")

        // when
        val result = exception.isRetriable()

        // then
        Assert.assertFalse(result)
    }

    @Test
    fun `exception with error code starting with 5 should not be treated as fatal`() {
        // given
        val exception = createConnectionException(500, 50000, "Retriable error")

        // when
        val result = exception.isFatal()

        // then
        Assert.assertFalse(result)
    }

    @Test
    fun `exception with error code starting with 5 should be treated as retriable`() {
        // given
        val exception = createConnectionException(500, 50000, "Retriable error")

        // when
        val result = exception.isRetriable()

        // then
        Assert.assertTrue(result)
    }

    private fun createConnectionException(code: Int, statusCode: Int, message: String): ConnectionException =
        ConnectionException(ErrorInformation(code, statusCode, message, null, null))
}
