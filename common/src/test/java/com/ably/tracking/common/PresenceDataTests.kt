package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.common.message.PresenceDataMessage
import com.google.common.truth.Truth
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.PresenceMessage
import org.junit.Assert
import org.junit.Test

class PresenceDataTests {
    private val gson = Gson()
    private val clientId = "TEST"

    @Test
    fun `return null if presence data is null`() {
        // given
        val presenceData = null
        val presenceMessage = PresenceMessage(PresenceMessage.Action.enter, clientId, presenceData)

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if presence data is in wrong format`() {
        // given
        val presenceData = "{\"wrongField\": \"wrong_value\"}"
        val presenceMessage = PresenceMessage(PresenceMessage.Action.enter, clientId, presenceData)

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `parse presence message if presence data is in correct format`() {
        // given
        val presenceData = gson.toJson(PresenceDataMessage("abc", null, null))
        val presenceMessage = PresenceMessage(PresenceMessage.Action.enter, clientId, presenceData)
        presenceMessage.connectionId = "TEST2"
        presenceMessage.id = "TEST3"

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNotNull(parsedMessage)
        Assert.assertNotNull(parsedMessage?.data)
        Assert.assertEquals("abc", parsedMessage?.data?.type)
        Assert.assertEquals(null, parsedMessage?.data?.resolution)
        Assert.assertEquals(null, parsedMessage?.data?.rawLocations)
    }

    @Test
    fun `parse presence message if presence data is in the correct JSON object format`() {
        val incomingPresenceMessage = """
            {
                "id": "6lxmVGvq-4:1:0",
                "clientId": "AatNetworkConnectivityTests_Subscriber",
                "connectionId": "6lxmVGvq-4",
                "timestamp": 1678439051717,
                "data": "{\"resolution\":{\"accuracy\":\"BALANCED\",\"desiredInterval\":1,\"minimumDisplacement\":0.0},\"type\":\"SUBSCRIBER\"}", "action": 4
            }
        """.trimIndent()
        val ablyJavaMessage = PresenceMessage.fromEncoded(incomingPresenceMessage, ChannelOptions())

        val trackingMessage: com.ably.tracking.common.PresenceMessage = ablyJavaMessage.toTracking(gson)!!

        Truth.assertThat(trackingMessage.timestamp).isEqualTo(1678439051717)
        Truth.assertThat(trackingMessage.id).isEqualTo("6lxmVGvq-4:1:0")
        Truth.assertThat(trackingMessage.memberKey).isEqualTo("6lxmVGvq-4:AatNetworkConnectivityTests_Subscriber")
        Truth.assertThat(trackingMessage.connectionId).isEqualTo("6lxmVGvq-4")
        Truth.assertThat(trackingMessage.clientId).isEqualTo("AatNetworkConnectivityTests_Subscriber")
        Truth.assertThat(trackingMessage.action).isEqualTo(PresenceAction.UPDATE)
        Truth.assertThat(trackingMessage.data.type).isEqualTo(ClientTypes.SUBSCRIBER)
        Truth.assertThat(trackingMessage.data.resolution)
            .isEqualTo(Resolution(Accuracy.BALANCED, 1, 0.0))
    }

    @Test
    fun `return null if presence data in wrong JSON object format`() {
        // given
        val presenceDataJson = JsonObject().apply {
            addProperty("wrongKey", "wrongValue")
        }
        val presenceMessage = PresenceMessage(PresenceMessage.Action.enter, clientId, presenceDataJson)

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNull(parsedMessage)
    }

    @Test
    fun `return null if presence data is of wrong type`() {
        // given
        val presenceData = 123
        val presenceMessage = PresenceMessage(PresenceMessage.Action.enter, clientId, presenceData)

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNull(parsedMessage)
    }
}
