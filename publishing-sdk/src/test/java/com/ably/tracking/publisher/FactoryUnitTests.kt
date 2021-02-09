package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LogConfiguration
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test
    fun `setting Ably connection config updates builder field`() {
        // given
        val configuration = ConnectionConfiguration("", "")

        // when
        val builder = Publisher.publishers().connection(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.connectionConfiguration)
    }

    @Test
    fun `setting Ably connection config returns a new copy of builder`() {
        // given
        val configuration = ConnectionConfiguration("", "")
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.connection(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting map config updates builder field`() {
        // given
        val configuration = MapConfiguration("")

        // when
        val builder = Publisher.publishers().map(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.mapConfiguration)
    }

    @Test
    fun `setting map config returns a new copy of builder`() {
        // given
        val configuration = MapConfiguration("")
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.map(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting logging config updates builder field`() {
        // given
        val configuration = LogConfiguration(true)

        // when
        val builder = Publisher.publishers().log(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.logConfiguration)
    }

    @Test
    fun `setting logging config returns a new copy of builder`() {
        // given
        val configuration = LogConfiguration(true)
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.log(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting debug config returns a new copy of builder`() {
        // given
        val configuration = DebugConfiguration()
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.debug(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting android context updates builder field`() {
        // given
        val mockedContext = mockk<Context>()

        // when
        val builder =
            Publisher.publishers().androidContext(mockedContext) as PublisherBuilder

        // then
        Assert.assertEquals(mockedContext, builder.androidContext)
    }

    @Test
    fun `setting android context returns a new copy of builder`() {
        // given
        val mockedContext = mockk<Context>()
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.androidContext(mockedContext)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting all data should update all builder fields`() {
        // given
        val builder = Publisher.publishers()
        assertAllBuilderFieldsAreNull(builder as PublisherBuilder)
        val mockedContext = mockk<Context>()

        // when
        val updatedBuilder = builder
            .connection(ConnectionConfiguration("", ""))
            .map(MapConfiguration(""))
            .log(LogConfiguration(true))
            .androidContext(mockedContext)

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
        Assert.assertNull(builder.mapConfiguration)
        Assert.assertNull(builder.logConfiguration)
        Assert.assertNull(builder.androidContext)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: PublisherBuilder) {
        Assert.assertNotNull(builder.connectionConfiguration)
        Assert.assertNotNull(builder.mapConfiguration)
        Assert.assertNotNull(builder.logConfiguration)
        Assert.assertNotNull(builder.androidContext)
    }

    private fun anyLocation() = Location("fused")
}
