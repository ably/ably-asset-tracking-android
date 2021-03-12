package com.ably.tracking.subscriber

import android.annotation.SuppressLint
import com.ably.tracking.Accuracy
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfigurationKey
import com.ably.tracking.Resolution
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class FactoryUnitTests {
    @Test
    fun `setting Ably connection config updates builder field`() {
        // given
        val configuration = ConnectionConfigurationKey("", "")

        // when
        val builder =
            Subscriber.subscribers().connection(configuration) as SubscriberBuilder

        // then
        Assert.assertEquals(configuration, builder.connectionConfiguration)
    }

    @Test
    fun `setting Ably connection config returns a new copy of builder`() {
        // given
        val configuration = ConnectionConfigurationKey("", "")
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.connection(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting resolution updates builder field`() {
        // given
        val resolution = Resolution(Accuracy.BALANCED, 333, 666.6)

        // when
        val builder = Subscriber.subscribers().resolution(resolution) as SubscriberBuilder

        // then
        Assert.assertEquals(resolution, builder.resolution)
    }

    @Test
    fun `setting resolution returns a new copy of builder`() {
        // given
        val resolution = Resolution(Accuracy.BALANCED, 333, 666.6)
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.resolution(resolution)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting tracking ID updates builder field`() {
        // given
        val trackingId = "abc"

        // when
        val builder = Subscriber.subscribers().trackingId(trackingId) as SubscriberBuilder

        // then
        Assert.assertEquals(trackingId, builder.trackingId)
    }

    @Test
    fun `setting tracking ID returns a new copy of builder`() {
        // given
        val trackingId = "abc"
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.trackingId(trackingId)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting all data should update all builder fields`() {
        // given
        val builder = Subscriber.subscribers()
        assertAllBuilderFieldsAreNull(builder as SubscriberBuilder)

        // when
        val updatedBuilder = builder
            .connection(ConnectionConfigurationKey("", ""))
            .resolution(Resolution(Accuracy.BALANCED, 333, 666.6))
            .trackingId("")

        // then
        assertAllBuilderFieldsAreNotNull(updatedBuilder as SubscriberBuilder)
    }

    @SuppressLint("MissingPermission")
    @Test(expected = BuilderConfigurationIncompleteException::class)
    fun `calling start with missing required fields should throw BuilderConfigurationIncompleteException`() {
        runBlocking {
            Subscriber.subscribers().start()
        }
    }

    private fun assertAllBuilderFieldsAreNull(builder: SubscriberBuilder) {
        Assert.assertNull(builder.connectionConfiguration)
        Assert.assertNull(builder.resolution)
        Assert.assertNull(builder.trackingId)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: SubscriberBuilder) {
        Assert.assertNotNull(builder.connectionConfiguration)
        Assert.assertNotNull(builder.resolution)
        Assert.assertNotNull(builder.trackingId)
    }
}
