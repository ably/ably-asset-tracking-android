package com.ably.tracking.common

import io.ably.lib.realtime.Channel.MessageListener
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ChannelEvent
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import io.ably.lib.realtime.Presence.PresenceListener
import io.ably.lib.rest.Auth.RenewAuthResult
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.types.PresenceMessage

interface AblySdkFactory<ChannelStateListenerType : AblySdkChannelStateListener> {
    fun createRealtime(clientOptions: ClientOptions): AblySdkRealtime<ChannelStateListenerType>

    /**
     * A channel state listener which [wrapChannelStateListener] can wrap to create a listener of type [ChannelStateListenerType].
     */
    fun interface UnderlyingChannelStateListener<ChannelStateListenerType : AblySdkChannelStateListener> {
        /**
         * This method is called whenever the [AblySdkChannelStateListener.onChannelStateChanged] method is called on an object [wrapper] which was the return value of calling [wrapChannelStateListener] with the receiver as the `underlyingListener` argument.
         *
         * @param wrapper The wrapper on which the [AblySdkChannelStateListener.onChannelStateChanged] method was called.
         * @param stateChange The `stateChange` argument received by [AblySdkChannelStateListener.onChannelStateChanged].
         */
        fun onChannelStateChanged(wrapper: ChannelStateListenerType, stateChange: AblySdkChannelStateListener.ChannelStateChange)
    }

    /**
     * Creates a channel state listener of type [ChannelStateListenerType], whose [AblySdkChannelStateListener.onChannelStateChanged] method calls [UnderlyingChannelStateListener.onChannelStateChanged] on [underlyingListener], passing the created listener as the `wrapper` argument.
     *
     * @param underlyingListener The listener to wrap.
     * @return A listener of type [ChannelStateListenerType] which wraps [underlyingListener].
     */
    fun wrapChannelStateListener(underlyingListener: UnderlyingChannelStateListener<ChannelStateListenerType>): ChannelStateListenerType
}

fun interface AblySdkChannelStateListener {
    interface ChannelStateChange {
        val event: ChannelEvent
        val current: ChannelState
        val previous: ChannelState
        val reason: ErrorInfo?
        val resumed: Boolean
    }

    fun onChannelStateChanged(stateChange: ChannelStateChange)
}

/**
 * An set of interfaces that represent the parts of the Ably client library which are used by the Ably Asset Tracking SDKs.
 *
 * These exist so that we can mock out the Ably client library when testing the [DefaultAbly] class. The interfaces here are more or less a direct copy of the corresponding `ably-java` classes, and are expected to exhibit the same behaviour. They do remove some details that would be overkill here, such as class hierarchies and interface conformances, instead opting to add these inherited properties and methods directly on the interfaces.
 *
 * The aim here is _not_ to remove the usage of all `ably-java` types; we continue using types from that codebase if they are just interfaces or are easy to construct. We just intend to use interfaces to replace `ably-java` classes that would be hard to mock.
 *
 * `ably-java` doesn’t currently have nullability annotations (see [issue #639](https://github.com/ably/ably-java/issues/639) there), so when writing these interfaces we need to make our own judgements about nullability, based on our knowledge of the behaviour of the Ably client libraries.
 */
interface AblySdkRealtime<ChannelStateListenerType : AblySdkChannelStateListener> {
    val auth: Auth
    val connection: Connection
    val channels: Channels<ChannelStateListenerType>

    fun connect()
    fun close()

    interface Auth {
        fun renewAuth(result: RenewAuthResult)
    }

    interface Channel<ChannelStateListenerType : AblySdkChannelStateListener> {
        val name: String
        val state: ChannelState
        val presence: Presence

        fun attach(listener: CompletionListener)
        fun publish(message: Message?, listener: CompletionListener)
        fun on(listener: ChannelStateListenerType)
        fun off(listener: ChannelStateListenerType)
        fun off()
        fun subscribe(name: String, listener: MessageListener)
        fun unsubscribe()
        fun setConnectionFailed(reason: ErrorInfo)
    }

    interface Presence {
        fun subscribe(listener: PresenceListener)
        fun unsubscribe()
        fun get(wait: Boolean): Array<PresenceMessage>
        fun enter(data: Any, listener: CompletionListener)
        fun update(data: Any, listener: CompletionListener)
        fun leave(data: Any, listener: CompletionListener)
    }

    interface Channels<ChannelStateListenerType : AblySdkChannelStateListener> {
        fun get(channelName: String, channelOptions: ChannelOptions?): Channel<ChannelStateListenerType>
        fun get(channelName: String): Channel<ChannelStateListenerType>
        fun entrySet(): Iterable<Map.Entry<String, Channel<ChannelStateListenerType>>>
        fun containsKey(key: Any): Boolean
        suspend fun release(channelName: String)

        fun offAll()
    }

    interface Connection {
        val state: ConnectionState
        val reason: ErrorInfo?

        fun on(listener: ConnectionStateListener)
        fun off(listener: ConnectionStateListener)

        fun offAll()
    }
}
