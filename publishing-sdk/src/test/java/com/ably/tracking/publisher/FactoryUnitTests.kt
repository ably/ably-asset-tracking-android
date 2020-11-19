package com.ably.tracking.publisher

import android.content.Context
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test
    fun `publishers should return AssetPublisher Builder object`() {
        val builder = AssetPublisher.publishers()
        Assert.assertTrue(builder is AssetPublisher.Builder)
    }

    @Test
    fun `setting Ably config updates builder field`() {
        val configuration = AblyConfiguration("")
        val builder = AssetPublisher.publishers().ablyConfig(configuration) as AssetPublisherBuilder
        Assert.assertEquals(configuration, builder.ablyConfiguration)
    }

    @Test
    fun `setting Ably config returns a new copy of builder`() {
        val configuration = AblyConfiguration("")
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.ablyConfig(configuration)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting map config updates builder field`() {
        val configuration = MapConfiguration("")
        val builder = AssetPublisher.publishers().mapConfig(configuration) as AssetPublisherBuilder
        Assert.assertEquals(configuration, builder.mapConfiguration)
    }

    @Test
    fun `setting map config returns a new copy of builder`() {
        val configuration = MapConfiguration("")
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.mapConfig(configuration)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting logging config updates builder field`() {
        val configuration = LogConfiguration(true)
        val builder = AssetPublisher.publishers().logConfig(configuration) as AssetPublisherBuilder
        Assert.assertEquals(configuration, builder.logConfiguration)
    }

    @Test
    fun `setting logging config returns a new copy of builder`() {
        val configuration = LogConfiguration(true)
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.logConfig(configuration)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting battery config updates builder field`() {
        val configuration = BatteryConfiguration("")
        val builder =
            AssetPublisher.publishers().batteryConfig(configuration) as AssetPublisherBuilder
        Assert.assertEquals(configuration, builder.batteryConfiguration)
    }

    @Test
    fun `setting battery config returns a new copy of builder`() {
        val configuration = BatteryConfiguration("")
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.batteryConfig(configuration)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting asset metadata JSON updates builder field`() {
        val metadata = ""
        val builder =
            AssetPublisher.publishers().assetMetadataJson(metadata) as AssetPublisherBuilder
        Assert.assertEquals(metadata, builder.assetMetadataJson)
    }

    @Test
    fun `setting asset metadata JSON returns a new copy of builder`() {
        val metadata = ""
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.assetMetadataJson(metadata)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting trip metadata JSON updates builder field`() {
        val metadata = ""
        val builder =
            AssetPublisher.publishers().tripMetadataJson(metadata) as AssetPublisherBuilder
        Assert.assertEquals(metadata, builder.tripMetadataJson)
    }

    @Test
    fun `setting trip metadata JSON returns a new copy of builder`() {
        val metadata = ""
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.tripMetadataJson(metadata)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting location updated listener updates builder field`() {
        val listener: LocationUpdatedListener = {}
        val builder =
            AssetPublisher.publishers().locationUpdatedListener(listener) as AssetPublisherBuilder
        Assert.assertEquals(listener, builder.locationUpdatedListener)
    }

    @Test
    fun `setting location updated listener returns a new copy of builder`() {
        val listener: LocationUpdatedListener = {}
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.locationUpdatedListener(listener)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting android context updates builder field`() {
        val mockedContext = mockk<Context>()
        val builder =
            AssetPublisher.publishers().androidContext(mockedContext) as AssetPublisherBuilder
        Assert.assertEquals(mockedContext, builder.androidContext)
    }

    @Test
    fun `setting android context returns a new copy of builder`() {
        val mockedContext = mockk<Context>()
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.androidContext(mockedContext)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting delivery data should update builder fields`() {
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"
        val builder = AssetPublisher.publishers()
            .delivery(trackingId, destination, vehicleType) as AssetPublisherBuilder
        Assert.assertEquals(trackingId, builder.trackingId)
        Assert.assertEquals(destination, builder.destination)
        Assert.assertEquals(vehicleType, builder.vehicleType)
    }

    @Test
    fun `setting delivery data returns a new copy of builder`() {
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"
        val originalBuilder = AssetPublisher.publishers()
        val newBuilder = originalBuilder.delivery(trackingId, destination, vehicleType)
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting all data should update all builder fields`() {
        val builder = AssetPublisher.publishers()
        assertAllBuilderFieldsAreNull(builder as AssetPublisherBuilder)
        val mockedContext = mockk<Context>()
        val updatedBuilder = builder
            .ablyConfig(AblyConfiguration(""))
            .mapConfig(MapConfiguration(""))
            .logConfig(LogConfiguration(true))
            .batteryConfig(BatteryConfiguration(""))
            .assetMetadataJson("")
            .tripMetadataJson("")
            .locationUpdatedListener { }
            .androidContext(mockedContext)
            .delivery("", "", "")
        assertAllBuilderFieldsAreNotNull(updatedBuilder as AssetPublisherBuilder)
    }

    @Test(expected = NotImplementedError::class)
    fun `calling start should throw NotImplementedError`() {
        AssetPublisher.publishers().start()
    }

    private fun assertAllBuilderFieldsAreNull(builder: AssetPublisherBuilder) {
        Assert.assertNull(builder.ablyConfiguration)
        Assert.assertNull(builder.mapConfiguration)
        Assert.assertNull(builder.logConfiguration)
        Assert.assertNull(builder.batteryConfiguration)
        Assert.assertNull(builder.assetMetadataJson)
        Assert.assertNull(builder.tripMetadataJson)
        Assert.assertNull(builder.locationUpdatedListener)
        Assert.assertNull(builder.androidContext)
        Assert.assertNull(builder.trackingId)
        Assert.assertNull(builder.destination)
        Assert.assertNull(builder.vehicleType)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: AssetPublisherBuilder) {
        Assert.assertNotNull(builder.ablyConfiguration)
        Assert.assertNotNull(builder.mapConfiguration)
        Assert.assertNotNull(builder.logConfiguration)
        Assert.assertNotNull(builder.batteryConfiguration)
        Assert.assertNotNull(builder.assetMetadataJson)
        Assert.assertNotNull(builder.tripMetadataJson)
        Assert.assertNotNull(builder.locationUpdatedListener)
        Assert.assertNotNull(builder.androidContext)
        Assert.assertNotNull(builder.trackingId)
        Assert.assertNotNull(builder.destination)
        Assert.assertNotNull(builder.vehicleType)
    }
}
