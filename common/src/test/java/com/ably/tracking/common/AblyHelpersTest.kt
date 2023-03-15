package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.google.gson.Gson
import io.ably.lib.types.ChannelOptions
import org.junit.Assert
import io.ably.lib.types.PresenceMessage as AblyJavaPresenceMessage
import org.junit.Test

class AblyHelpersTest {
    @Test
    fun itConvertsPresenceMessagesToTracking()
    {
        val gson = Gson();
        val incomingPresenceMessage = """
            {
                "id": "6lxmVGvq-4:1:0",
                "clientId": "AatNetworkConnectivityTests_Subscriber",
                "connectionId": "6lxmVGvq-4",
                "timestamp": 1678439051717,
                "data": "{\"resolution\":{\"accuracy\":\"BALANCED\",\"desiredInterval\":1,\"minimumDisplacement\":0.0},\"type\":\"SUBSCRIBER\"}", "action": 4
            }
        """.trimIndent()
        val ablyJavaMessage = AblyJavaPresenceMessage.fromEncoded(incomingPresenceMessage, ChannelOptions())

        val trackingMessage: PresenceMessage = ablyJavaMessage.toTracking(gson)!!

        Assert.assertEquals(1678439051717, trackingMessage.timestamp)
        Assert.assertEquals("6lxmVGvq-4:AatNetworkConnectivityTests_Subscriber", trackingMessage.memberKey)
        Assert.assertEquals(PresenceAction.UPDATE, trackingMessage.action)
        Assert.assertEquals(ClientTypes.SUBSCRIBER, trackingMessage.data.type)
        Assert.assertEquals(Resolution(Accuracy.BALANCED, 1, 0.0), trackingMessage.data.resolution)
    }
}
