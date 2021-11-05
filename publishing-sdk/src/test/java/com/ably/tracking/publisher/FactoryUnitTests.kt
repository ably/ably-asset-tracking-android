package com.ably.tracking.publisher

import android.annotation.SuppressLint
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import org.junit.Assert
import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test
    fun `setting Ably connection config updates builder field`() {
        // given
        val configuration = ConnectionConfiguration(Authentication.basic("", ""))

        // when
        val builder = Publisher.publishers().connection(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.connectionConfiguration)
    }

    @Test
    fun `setting Ably connection config returns a new copy of builder`() {
        // given
        val configuration = ConnectionConfiguration(Authentication.basic("", ""))
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.connection(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting all data should update all builder fields`() {
        // given
        val builder = Publisher.publishers()
        assertAllBuilderFieldsAreNull(builder as PublisherBuilder)

        // when
        val updatedBuilder = builder
            .connection(ConnectionConfiguration(Authentication.basic("", "")))

        // then
        assertAllBuilderFieldsAreNotNull(updatedBuilder as PublisherBuilder)
    }

    @SuppressLint("MissingPermission")
    @Test(expected = BuilderConfigurationIncompleteException::class)
    fun `calling start with missing required fields should throw BuilderConfigurationIncompleteException`() {
        Publisher.publishers().start()
    }

    private fun assertAllBuilderFieldsAreNull(builder: PublisherBuilder) {
        Assert.assertNull(builder.connectionConfiguration)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: PublisherBuilder) {
        Assert.assertNotNull(builder.connectionConfiguration)
    }
}
