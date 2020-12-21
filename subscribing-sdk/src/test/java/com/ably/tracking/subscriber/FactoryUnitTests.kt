package com.ably.tracking.subscriber

import android.annotation.SuppressLint
import android.location.Location
import com.ably.tracking.Accuracy
import com.ably.tracking.AssetStatusListener
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LocationUpdatedListener
import com.ably.tracking.LogConfiguration
import com.ably.tracking.Resolution
import org.junit.Assert
import org.junit.Test

class FactoryUnitTests {
    @Test
    fun `setting Ably connection config updates builder field`() {
        // given
        val configuration = ConnectionConfiguration("", "")

        // when
        val builder =
            Subscriber.subscribers().connection(configuration) as SubscriberBuilder

        // then
        Assert.assertEquals(configuration, builder.connectionConfiguration)
    }

    @Test
    fun `setting Ably connection config returns a new copy of builder`() {
        // given
        val configuration = ConnectionConfiguration("", "")
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.connection(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting logging config updates builder field`() {
        // given
        val configuration = LogConfiguration(true)

        // when
        val builder =
            Subscriber.subscribers().log(configuration) as SubscriberBuilder

        // then
        Assert.assertEquals(configuration, builder.logConfiguration)
    }

    @Test
    fun `setting logging config returns a new copy of builder`() {
        // given
        val configuration = LogConfiguration(true)
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.log(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting raw location updated listener updates builder field`() {
        // given
        val listener: LocationUpdatedListener = anyLocationUpdatedListener()

        // when
        val builder =
            Subscriber.subscribers()
                .rawLocationUpdatedListener(listener) as SubscriberBuilder

        // then
        Assert.assertEquals(listener, builder.rawLocationUpdatedListener)
    }

    @Test
    fun `setting raw location updated listener returns a new copy of builder`() {
        // given
        val listener: LocationUpdatedListener = anyLocationUpdatedListener()
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.rawLocationUpdatedListener(listener)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting enhanced location updated listener updates builder field`() {
        // given
        val listener: LocationUpdatedListener = anyLocationUpdatedListener()

        // when
        val builder =
            Subscriber.subscribers()
                .enhancedLocationUpdatedListener(listener) as SubscriberBuilder

        // then
        Assert.assertEquals(listener, builder.enhancedLocationUpdatedListener)
    }

    @Test
    fun `setting enhanced location updated listener returns a new copy of builder`() {
        // given
        val listener: LocationUpdatedListener = anyLocationUpdatedListener()
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.enhancedLocationUpdatedListener(listener)

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
    fun `setting asset status listener updates builder field`() {
        // given
        val listener: AssetStatusListener = anyAssetStatusListener()

        // when
        val builder =
            Subscriber.subscribers().assetStatusListener(listener) as SubscriberBuilder

        // then
        Assert.assertEquals(listener, builder.assetStatusListener)
    }

    @Test
    fun `setting asset status listener returns a new copy of builder`() {
        // given
        val listener: AssetStatusListener = anyAssetStatusListener()
        val originalBuilder = Subscriber.subscribers()

        // when
        val newBuilder = originalBuilder.assetStatusListener(listener)

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
            .connection(ConnectionConfiguration("", ""))
            .log(LogConfiguration(true))
            .rawLocationUpdatedListener(anyLocationUpdatedListener())
            .enhancedLocationUpdatedListener(anyLocationUpdatedListener())
            .resolution(Resolution(Accuracy.BALANCED, 333, 666.6))
            .trackingId("")

        // then
        assertAllBuilderFieldsAreNotNull(updatedBuilder as SubscriberBuilder)
    }

    @SuppressLint("MissingPermission")
    @Test(expected = BuilderConfigurationIncompleteException::class)
    fun `calling start with missing required fields should throw BuilderConfigurationIncompleteException`() {
        Subscriber.subscribers().start()
    }

    private fun assertAllBuilderFieldsAreNull(builder: SubscriberBuilder) {
        Assert.assertNull(builder.connectionConfiguration)
        Assert.assertNull(builder.logConfiguration)
        Assert.assertNull(builder.rawLocationUpdatedListener)
        Assert.assertNull(builder.enhancedLocationUpdatedListener)
        Assert.assertNull(builder.resolution)
        Assert.assertNull(builder.trackingId)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: SubscriberBuilder) {
        Assert.assertNotNull(builder.connectionConfiguration)
        Assert.assertNotNull(builder.logConfiguration)
        Assert.assertNotNull(builder.rawLocationUpdatedListener)
        Assert.assertNotNull(builder.enhancedLocationUpdatedListener)
        Assert.assertNotNull(builder.resolution)
        Assert.assertNotNull(builder.trackingId)
    }

    private fun anyLocationUpdatedListener(): LocationUpdatedListener = object : LocationUpdatedListener {
        override fun onLocationUpdated(location: Location) = Unit
    }

    private fun anyAssetStatusListener(): AssetStatusListener = object : AssetStatusListener {
        override fun onStatusChanged(isOnline: Boolean) = Unit
    }
}
