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
        // given

        // when
        val builder = AssetPublisher.publishers()

        // then
        Assert.assertTrue(builder is AssetPublisher.Builder)
    }

    @Test
    fun `setting Ably config updates builder field`() {
        // given
        val configuration = AblyConfiguration("")

        // when
        val builder = AssetPublisher.publishers().ablyConfig(configuration) as AssetPublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.ablyConfiguration)
    }

    @Test
    fun `setting Ably config returns a new copy of builder`() {
        // given
        val configuration = AblyConfiguration("")
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.ablyConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting map config updates builder field`() {
        // given
        val configuration = MapConfiguration("")

        // when
        val builder = AssetPublisher.publishers().mapConfig(configuration) as AssetPublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.mapConfiguration)
    }

    @Test
    fun `setting map config returns a new copy of builder`() {
        // given
        val configuration = MapConfiguration("")
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.mapConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting logging config updates builder field`() {
        // given
        val configuration = LogConfiguration(true)

        // when
        val builder = AssetPublisher.publishers().logConfig(configuration) as AssetPublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.logConfiguration)
    }

    @Test
    fun `setting logging config returns a new copy of builder`() {
        // given
        val configuration = LogConfiguration(true)
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.logConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting battery config updates builder field`() {
        // given
        val configuration = BatteryConfiguration("")

        // when
        val builder =
            AssetPublisher.publishers().batteryConfig(configuration) as AssetPublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.batteryConfiguration)
    }

    @Test
    fun `setting battery config returns a new copy of builder`() {
        // given
        val configuration = BatteryConfiguration("")
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.batteryConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting asset metadata JSON updates builder field`() {
        // given
        val metadata = ""

        // when
        val builder =
            AssetPublisher.publishers().assetMetadataJson(metadata) as AssetPublisherBuilder

        // then
        Assert.assertEquals(metadata, builder.assetMetadataJson)
    }

    @Test
    fun `setting asset metadata JSON returns a new copy of builder`() {
        // given
        val metadata = ""
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.assetMetadataJson(metadata)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting trip metadata JSON updates builder field`() {
        // given
        val metadata = ""

        // when
        val builder =
            AssetPublisher.publishers().tripMetadataJson(metadata) as AssetPublisherBuilder

        // then
        Assert.assertEquals(metadata, builder.tripMetadataJson)
    }

    @Test
    fun `setting trip metadata JSON returns a new copy of builder`() {
        // given
        val metadata = ""
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.tripMetadataJson(metadata)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting location updated listener updates builder field`() {
        // given
        val listener: LocationUpdatedListener = {}

        // when
        val builder =
            AssetPublisher.publishers().locationUpdatedListener(listener) as AssetPublisherBuilder

        // then
        Assert.assertEquals(listener, builder.locationUpdatedListener)
    }

    @Test
    fun `setting location updated listener returns a new copy of builder`() {
        // given
        val listener: LocationUpdatedListener = {}
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.locationUpdatedListener(listener)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting android context updates builder field`() {
        // given
        val mockedContext = mockk<Context>()

        // when
        val builder =
            AssetPublisher.publishers().androidContext(mockedContext) as AssetPublisherBuilder

        // then
        Assert.assertEquals(mockedContext, builder.androidContext)
    }

    @Test
    fun `setting android context returns a new copy of builder`() {
        // given
        val mockedContext = mockk<Context>()
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.androidContext(mockedContext)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting delivery data should update builder fields`() {
        // given
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"

        // when
        val builder = AssetPublisher.publishers()
            .delivery(trackingId, destination, vehicleType) as AssetPublisherBuilder

        // then
        Assert.assertEquals(trackingId, builder.trackingId)
        Assert.assertEquals(destination, builder.destination)
        Assert.assertEquals(vehicleType, builder.vehicleType)
    }

    @Test
    fun `setting delivery data returns a new copy of builder`() {
        // given
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"
        val originalBuilder = AssetPublisher.publishers()

        // when
        val newBuilder = originalBuilder.delivery(trackingId, destination, vehicleType)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting all data should update all builder fields`() {
        // given
        val builder = AssetPublisher.publishers()
        assertAllBuilderFieldsAreNull(builder as AssetPublisherBuilder)
        val mockedContext = mockk<Context>()

        // when
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

        // then
        assertAllBuilderFieldsAreNotNull(updatedBuilder as AssetPublisherBuilder)
    }

    @Test(expected = BuilderConfigurationIncompleteException::class)
    fun `calling start with missing required fields should throw BuilderConfigurationIncompleteException`() {
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
