package com.ably.tracking.common

import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.message.getEnhancedLocationUpdate
import com.ably.tracking.common.message.getRawLocationUpdate
import com.ably.tracking.test.common.anyLocationMessage
import com.google.gson.Gson
import io.ably.lib.types.Message
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MessageValidationTests(locationUpdatesType: String) {
    private val gson = Gson()
    private val isTestingEnhancedLocationUpdates = locationUpdatesType == ENHANCED

    companion object {
        private const val ENHANCED = "ENHANCED"
        private const val RAW = "RAW"

        @JvmStatic
        @Parameterized.Parameters(name = "Deserializing {0} location updates")
        fun data() = listOf(arrayOf(ENHANCED), arrayOf(RAW))
    }

    @Test
    fun `return null if message data is null`() {
        // given
        val messageData = null

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is not a string`() {
        // given
        val messageData = 123

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is empty`() {
        // given
        val messageData = ""

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is an empty JSON`() {
        // given
        val messageData = "{}"

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is not complete`() {
        // given
        val messageData = createJsonObjectString(createIncompleteMessageDataParts())

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but the type value is invalid`() {
        // Enhanced location update specific test case
        Assume.assumeTrue(isTestingEnhancedLocationUpdates)

        // given
        val invalidTypeJsonPart = "\"type\":\"INVALID_VALUE\""
        val messageData = createJsonObjectString(
            listOf(
                anyLocationJsonPart,
                emptySkippedLocationsJsonPart,
                emptyIntermediateLocationsJsonPart,
                invalidTypeJsonPart,
            )
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains invalid location value for the location`() {
        // given
        val invalidLocationJsonPart = "\"location\":123"
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(invalidLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains empty object for the location`() {
        // given
        val emptyLocationJsonPart = "\"location\":{}"
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(emptyLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains an incomplete location object`() {
        // given
        val incompleteLocationJsonPart = """
            "location":{
                "type":"Feature"
            }
        """.trimIndent()
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(incompleteLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains an incomplete geometry object inside the location object`() {
        // given
        println(anyLocationJsonPart)
        val incompleteLocationJsonPart = """
            "location":{
                "type":"Feature",
                "geometry":{},
                "properties":{"accuracyHorizontal":1.0,"bearing":2.0,"speed":3.0,"time":4.0}
            }
        """.trimIndent()
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(incompleteLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains too few coordinates inside the location object`() {
        // given
        println(anyLocationJsonPart)
        val incompleteLocationJsonPart = """
            "location":{
                "type":"Feature",
                "geometry":{"type":"Point","coordinates":[1.0,2.0]},
                "properties":{"accuracyHorizontal":1.0,"bearing":2.0,"speed":3.0,"time":4.0}
            }
        """.trimIndent()
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(incompleteLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains values of wrong type in any locations list`() {
        // given
        val invalidSkippedLocationsJsonPart = "\"skippedLocations\":[1,2,3]"
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithSkippedLocations(invalidSkippedLocationsJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains a null inside any locations list`() {
        // given
        val invalidSkippedLocationsJsonPart = "\"skippedLocations\":[null]"
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithSkippedLocations(invalidSkippedLocationsJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if message data is complete but contains empty objects inside any locations list`() {
        // given
        val invalidSkippedLocationsJsonPart = "\"skippedLocations\":[{},{}]"
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithSkippedLocations(invalidSkippedLocationsJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `deserialize the message data if it is complete`() {
        // given
        val messageData = createJsonObjectString(
            createCompleteMessageDataParts()
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNotNull(parsedMessage)
    }

    @Test
    fun `deserialize the message data if it is complete and contains extra fields`() {
        // given
        val extraJsonPart = "\"someExtraField\":\"EXTRA_VALUE\""
        val messageData = createJsonObjectString(
            createCompleteMessageDataParts() + extraJsonPart
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNotNull(parsedMessage)
    }

    @Test
    fun `deserialize the message data if it is complete and is missing primitive values inside the location object`() {
        // given
        println(anyLocationJsonPart)
        val incompleteLocationJsonPart = """
            "location":{
                "type":"Feature",
                "geometry":{"type":"Point","coordinates":[1.0,2.0,3.0]},
                "properties":{}
            }
        """.trimIndent()
        val messageData = createJsonObjectString(
            createCompleteMessageDataPartsWithLocation(incompleteLocationJsonPart)
        )

        // when
        val parsedMessage = deserializeLocationUpdate(messageData)

        // then
        Assert.assertNotNull(parsedMessage)
    }

    private fun deserializeLocationUpdate(messageData: Any?): LocationUpdate? =
        createMessage(messageData).let {
            if (isTestingEnhancedLocationUpdates) {
                it.getEnhancedLocationUpdate(gson)
            } else {
                it.getRawLocationUpdate(gson)
            }
        }

    private fun createMessage(messageData: Any?): Message = Message("test-name", messageData)

    private fun createJsonObjectString(parts: List<String>): String =
        "{${parts.joinToString(separator = ",")}}"

    private fun createCompleteMessageDataParts(): List<String> =
        mutableListOf(
            anyLocationJsonPart,
            emptySkippedLocationsJsonPart,
        ).addEnhancedLocationUpdateParts()

    private fun createIncompleteMessageDataParts(): List<String> =
        mutableListOf(
            anyLocationJsonPart,
        ).addEnhancedLocationUpdateParts()

    private fun createCompleteMessageDataPartsWithLocation(locationJsonPart: String): List<String> =
        mutableListOf(
            locationJsonPart,
            emptySkippedLocationsJsonPart,
        ).addEnhancedLocationUpdateParts()

    private fun createCompleteMessageDataPartsWithSkippedLocations(skippedLocationsJsonPart: String): List<String> =
        mutableListOf(
            anyLocationJsonPart,
            skippedLocationsJsonPart,
        ).addEnhancedLocationUpdateParts()

    private fun MutableList<String>.addEnhancedLocationUpdateParts(): List<String> =
        apply {
            if (isTestingEnhancedLocationUpdates) {
                add(emptyIntermediateLocationsJsonPart)
                add(anyTypeJsonPart)
            }
        }

    private val anyLocationJsonPart = "\"location\":${gson.toJson(anyLocationMessage())}"
    private val emptySkippedLocationsJsonPart = "\"skippedLocations\":[]"
    private val emptyIntermediateLocationsJsonPart = "\"intermediateLocations\":[]"
    private val anyTypeJsonPart = "\"type\":\"ACTUAL\""
}
