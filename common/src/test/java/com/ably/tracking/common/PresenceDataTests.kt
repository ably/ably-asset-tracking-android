package com.ably.tracking.common

import com.ably.tracking.common.message.PresenceDataMessage
import com.google.gson.Gson
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

        // when
        val parsedMessage = presenceMessage.toTracking(gson)

        // then
        Assert.assertNotNull(parsedMessage)
        Assert.assertNotNull(parsedMessage?.data)
        Assert.assertEquals("abc", parsedMessage?.data?.type)
        Assert.assertEquals(null, parsedMessage?.data?.resolution)
        Assert.assertEquals(null, parsedMessage?.data?.rawLocations)
    }
}
