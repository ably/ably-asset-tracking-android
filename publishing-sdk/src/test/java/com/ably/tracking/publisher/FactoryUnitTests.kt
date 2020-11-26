package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.content.Context
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test
    fun `publishers should return Publisher Builder object`() {
        // given

        // when
        val builder = Publisher.publishers()

        // then
        Assert.assertTrue(builder is Publisher.Builder)
    }

    @Test
    fun `setting Ably config updates builder field`() {
        // given
        val configuration = AblyConfiguration("", "")

        // when
        val builder = Publisher.publishers().ablyConfig(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.ablyConfiguration)
    }

    @Test
    fun `setting Ably config returns a new copy of builder`() {
        // given
        val configuration = AblyConfiguration("", "")
        val originalBuilder = Publisher.publishers()

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
        val builder = Publisher.publishers().mapConfig(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.mapConfiguration)
    }

    @Test
    fun `setting map config returns a new copy of builder`() {
        // given
        val configuration = MapConfiguration("")
        val originalBuilder = Publisher.publishers()

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
        val builder = Publisher.publishers().logConfig(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.logConfiguration)
    }

    @Test
    fun `setting logging config returns a new copy of builder`() {
        // given
        val configuration = LogConfiguration(true)
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.logConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting debug config updates builder field`() {
        // given
        val configuration = DebugConfiguration({}, LocationSourceAbly(""))

        // when
        val builder =
            Publisher.publishers().debugConfig(configuration) as PublisherBuilder

        // then
        Assert.assertEquals(configuration, builder.debugConfiguration)
    }

    @Test
    fun `setting debug config returns a new copy of builder`() {
        // given
        val configuration = DebugConfiguration()
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.debugConfig(configuration)

        // then
        Assert.assertNotEquals(newBuilder, originalBuilder)
    }

    @Test
    fun `setting asset metadata JSON updates builder field`() {
        // given
        val metadata = ""

        // when
        val builder =
            Publisher.publishers().assetMetadataJson(metadata) as PublisherBuilder

        // then
        Assert.assertEquals(metadata, builder.assetMetadataJson)
    }

    @Test
    fun `setting asset metadata JSON returns a new copy of builder`() {
        // given
        val metadata = ""
        val originalBuilder = Publisher.publishers()

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
            Publisher.publishers().tripMetadataJson(metadata) as PublisherBuilder

        // then
        Assert.assertEquals(metadata, builder.tripMetadataJson)
    }

    @Test
    fun `setting trip metadata JSON returns a new copy of builder`() {
        // given
        val metadata = ""
        val originalBuilder = Publisher.publishers()

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
            Publisher.publishers().locationUpdatedListener(listener) as PublisherBuilder

        // then
        Assert.assertEquals(listener, builder.locationUpdatedListener)
    }

    @Test
    fun `setting location updated listener returns a new copy of builder`() {
        // given
        val listener: LocationUpdatedListener = {}
        val originalBuilder = Publisher.publishers()

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
    fun `setting delivery data should update builder fields`() {
        // given
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"

        // when
        val builder = Publisher.publishers()
            .delivery(trackingId, destination, vehicleType) as PublisherBuilder

        // then
        Assert.assertEquals(trackingId, builder.trackingId)
        Assert.assertEquals(destination, builder.destination)
        Assert.assertEquals(vehicleType, builder.vehicleType)
    }

    @Test
    fun `passing nulls as optional params of delivery data should set default values for those fields`() {
        // given
        val trackingId = "abc"

        // when
        val builder = Publisher.publishers()
            .delivery(trackingId, null, null) as PublisherBuilder

        // then
        Assert.assertEquals(trackingId, builder.trackingId)
        Assert.assertEquals("", builder.destination)
        Assert.assertEquals("car", builder.vehicleType)
    }

    @Test
    fun `setting only the required params of delivery data should set default values for optional fields`() {
        // given
        val trackingId = "abc"

        // when
        val builder = Publisher.publishers()
            .delivery(trackingId) as PublisherBuilder

        // then
        Assert.assertEquals(trackingId, builder.trackingId)
        Assert.assertEquals("", builder.destination)
        Assert.assertEquals("car", builder.vehicleType)
    }

    @Test
    fun `setting delivery data returns a new copy of builder`() {
        // given
        val trackingId = "abc"
        val destination = "London"
        val vehicleType = "car"
        val originalBuilder = Publisher.publishers()

        // when
        val newBuilder = originalBuilder.delivery(trackingId, destination, vehicleType)

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
            .ablyConfig(AblyConfiguration("", ""))
            .mapConfig(MapConfiguration(""))
            .logConfig(LogConfiguration(true))
            .assetMetadataJson("")
            .tripMetadataJson("")
            .locationUpdatedListener { }
            .androidContext(mockedContext)
            .delivery("", "", "")

        // then
        assertAllBuilderFieldsAreNotNull(updatedBuilder as PublisherBuilder)
    }

    @SuppressLint("MissingPermission")
    @Test(expected = BuilderConfigurationIncompleteException::class)
    fun `calling start with missing required fields should throw BuilderConfigurationIncompleteException`() {
        Publisher.publishers().start()
    }

    private fun assertAllBuilderFieldsAreNull(builder: PublisherBuilder) {
        Assert.assertNull(builder.ablyConfiguration)
        Assert.assertNull(builder.mapConfiguration)
        Assert.assertNull(builder.logConfiguration)
        Assert.assertNull(builder.assetMetadataJson)
        Assert.assertNull(builder.tripMetadataJson)
        Assert.assertNull(builder.locationUpdatedListener)
        Assert.assertNull(builder.androidContext)
        Assert.assertNull(builder.trackingId)
        Assert.assertNull(builder.destination)
        Assert.assertNull(builder.vehicleType)
    }

    private fun assertAllBuilderFieldsAreNotNull(builder: PublisherBuilder) {
        Assert.assertNotNull(builder.ablyConfiguration)
        Assert.assertNotNull(builder.mapConfiguration)
        Assert.assertNotNull(builder.logConfiguration)
        Assert.assertNotNull(builder.assetMetadataJson)
        Assert.assertNotNull(builder.tripMetadataJson)
        Assert.assertNotNull(builder.locationUpdatedListener)
        Assert.assertNotNull(builder.androidContext)
        Assert.assertNotNull(builder.trackingId)
        Assert.assertNotNull(builder.destination)
        Assert.assertNotNull(builder.vehicleType)
    }
}
