package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.PresenceMessage as AblyJavaPresenceMessage
import org.junit.Test

class AblyHelpersTest {
    @Test
    fun itConvertsPresenceMessagesToTracking() {
        val gson = Gson()
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

        assertThat(trackingMessage.timestamp).isEqualTo(1678439051717)
        assertThat(trackingMessage.memberKey).isEqualTo("6lxmVGvq-4:AatNetworkConnectivityTests_Subscriber")
        assertThat(trackingMessage.action).isEqualTo(PresenceAction.UPDATE)
        assertThat(trackingMessage.data.type).isEqualTo(ClientTypes.SUBSCRIBER)
        assertThat(trackingMessage.data.resolution).isEqualTo(Resolution(Accuracy.BALANCED, 1, 0.0))
    }
}
