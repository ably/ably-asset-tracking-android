package com.ably.tracking.publisher

import com.google.gson.Gson
import org.junit.Assert
import org.junit.Test

class GeoJsonMappersTest {
    @Test
    fun `mapping single GeoJsonMessage to JSON Array`() {
        // given
        val gson = Gson()
        val geoJsonMessage = GeoJsonMessage(
            GeoJsonTypes.FEATURE,
            GeoJsonGeometry(GeoJsonTypes.POINT, listOf(1.0, 2.0)),
            GeoJsonProperties(1F, 1.0, 2F, 3F, 2.0)
        )

        // when
        val jsonString = geoJsonMessage.toJsonArray(gson)

        // then
        Assert.assertEquals(
            "[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1.0,2.0]},\"properties\":{\"accuracyHorizontal\":1.0,\"altitude\":1.0,\"bearing\":2.0,\"speed\":3.0,\"time\":2.0}}]",
            jsonString
        )
    }

    @Test
    fun `mapping list of GeoJsonMessages to JSON Array`() {
        // given
        val gson = Gson()
        val geoJsonMessage = GeoJsonMessage(
            GeoJsonTypes.FEATURE,
            GeoJsonGeometry(GeoJsonTypes.POINT, listOf(1.0, 2.0)),
            GeoJsonProperties(1F, 1.0, 2F, 3F, 2.0)
        )
        val geoJsonMessages = listOf(geoJsonMessage, geoJsonMessage)

        // when
        val jsonString = geoJsonMessages.toJsonArray(gson)

        // then
        Assert.assertEquals(
            "[" +
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1.0,2.0]},\"properties\":{\"accuracyHorizontal\":1.0,\"altitude\":1.0,\"bearing\":2.0,\"speed\":3.0,\"time\":2.0}}," +
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1.0,2.0]},\"properties\":{\"accuracyHorizontal\":1.0,\"altitude\":1.0,\"bearing\":2.0,\"speed\":3.0,\"time\":2.0}}" +
                "]",
            jsonString
        )
    }
}
