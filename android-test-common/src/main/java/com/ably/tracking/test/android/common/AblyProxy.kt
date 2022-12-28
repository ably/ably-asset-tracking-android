package com.ably.tracking.test.android.common

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions

sealed class ProtocolAction {
    /**
     * `presence.enter` realtime protocol action
     */
    object PresenceEnter : ProtocolAction()
    // ...
}

sealed class ProxyResponse {
    /**
     * Terminate the connection when configured action has been detected
     */
    object Disconnect : ProxyResponse()
    // ...
}

interface RealtimeProxy  {
    /**
     * Return an AblyRealtime client that has been congfigured to pass traffic
     * through the proxy
     */
    fun proxy(opts: ClientOptions) : AblyRealtime

    /**
     * Configure proxy to give specified response when given protocol action
     * occurs across proxied traffic. All other actions pass through.
     */
    fun onAction(action: ProtocolAction, response: ProxyResponse)

    /**
     * Reset any configured responses/actions so that all traffic is back to pass-through.
     */
    fun reset()
}
