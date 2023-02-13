package com.ably.tracking.common

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel.MessageListener
import io.ably.lib.realtime.ChannelEvent
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ChannelStateListener
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * An implementation of [AblySdkFactory] which uses the `ably-java` client library.
 */
class DefaultAblySdkFactory : AblySdkFactory<DefaultAblySdkChannelStateListener> {
    override fun createRealtime(clientOptions: ClientOptions): AblySdkRealtime<DefaultAblySdkChannelStateListener> {
        return DefaultAblySdkRealtime(clientOptions)
    }

    override fun wrapChannelStateListener(underlyingListener: AblySdkFactory.UnderlyingChannelStateListener<DefaultAblySdkChannelStateListener>): DefaultAblySdkChannelStateListener {
        return DefaultAblySdkChannelStateListener(underlyingListener)
    }
}

class DefaultAblySdkChannelStateListener(private val underlyingListener: AblySdkFactory.UnderlyingChannelStateListener<DefaultAblySdkChannelStateListener>) :
    AblySdkChannelStateListener {
    val ablyJavaListener = ChannelStateListener { stateChange ->
        onChannelStateChanged(
            object : AblySdkChannelStateListener.ChannelStateChange {
                override val event: ChannelEvent
                    get() = stateChange.event
                override val current: ChannelState
                    get() = stateChange.current
                override val previous: ChannelState
                    get() = stateChange.previous
                override val reason: ErrorInfo?
                    get() = stateChange.reason
                override val resumed: Boolean
                    get() = stateChange.resumed
            }
        )
    }

    override fun onChannelStateChanged(stateChange: AblySdkChannelStateListener.ChannelStateChange) {
        underlyingListener.onChannelStateChanged(this, stateChange)
    }
}

class DefaultAblySdkRealtime
constructor(clientOptions: ClientOptions) : AblySdkRealtime<DefaultAblySdkChannelStateListener> {
    private val realtime = AblyRealtime(clientOptions)

    override val auth = Auth(realtime.auth)
    override val connection = Connection(realtime.connection)
    override val channels = Channels(realtime.channels)

    override fun connect() {
        realtime.connect()
    }

    override fun close() {
        realtime.close()
    }

    class Auth
    constructor(private val auth: io.ably.lib.rest.Auth) : AblySdkRealtime.Auth {
        override fun renewAuth(result: RenewAuthResult) {
            auth.renewAuth(result)
        }
    }

    class Connection
    constructor(private val connection: io.ably.lib.realtime.Connection) :
        AblySdkRealtime.Connection {
        override val state: ConnectionState
            get() = connection.state
        override val reason: ErrorInfo?
            get() = connection.reason

        override fun on(listener: ConnectionStateListener) {
            connection.on(listener)
        }

        override fun off(listener: ConnectionStateListener) {
            connection.off(listener)
        }

        override fun offAll() {
            connection.off()
        }
    }

    class Channels
    constructor(private val channels: AblyRealtime.Channels) : AblySdkRealtime.Channels<DefaultAblySdkChannelStateListener> {
        override fun get(
            channelName: String,
            channelOptions: ChannelOptions?
        ): AblySdkRealtime.Channel<DefaultAblySdkChannelStateListener> {
            return Channel(channels.get(channelName, channelOptions))
        }

        override fun get(channelName: String): AblySdkRealtime.Channel<DefaultAblySdkChannelStateListener> {
            return Channel(channels.get(channelName))
        }

        override fun containsKey(key: Any): Boolean {
            return channels.containsKey(key)
        }

        override suspend fun release(channelName: String) {

            var detached = false
            channels.get(channelName).detach(object : CompletionListener {
                override fun onSuccess() {
                    detached = true
                }

                override fun onError(reason: ErrorInfo?) {
                    detached = false
                }

            })

            while (!detached) {
                delay(50)
            }

            channels.release(channelName)
        }

        override fun offAll() {
           channels.values().forEach{
               it.off()
           }
        }

        override fun entrySet(): Iterable<Map.Entry<String, AblySdkRealtime.Channel<DefaultAblySdkChannelStateListener>>> {
            return channels.entrySet().map { entry ->
                object : Map.Entry<String, AblySdkRealtime.Channel<DefaultAblySdkChannelStateListener>> {
                    override val key = entry.key
                    override val value = Channel(entry.value)
                }
            }
        }
    }

    class Channel
    constructor(private val channel: io.ably.lib.realtime.Channel) : AblySdkRealtime.Channel<DefaultAblySdkChannelStateListener> {
        override val name = channel.name
        override val state: ChannelState
            get() = channel.state
        override val presence = Presence(channel.presence)

        override fun attach(listener: CompletionListener) {
            channel.attach(listener)
        }

        override fun on(listener: DefaultAblySdkChannelStateListener) {
            channel.on(listener.ablyJavaListener)
        }

        override fun off(listener: DefaultAblySdkChannelStateListener) {
            channel.off(listener.ablyJavaListener)
        }

        override fun off() {
            channel.off()
        }

        override fun publish(message: Message?, listener: CompletionListener) {
            channel.publish(message, listener)
        }

        override fun subscribe(name: String, listener: MessageListener) {
            channel.subscribe(name, listener)
        }

        override fun unsubscribe() {
            channel.unsubscribe()
        }

        override fun setConnectionFailed(reason: ErrorInfo) {
            channel.setConnectionFailed(reason)
        }

        class Presence
        constructor(private val presence: io.ably.lib.realtime.Presence) :
            AblySdkRealtime.Presence {
            override fun subscribe(listener: PresenceListener) {
                presence.subscribe(listener)
            }

            override fun unsubscribe() {
                presence.unsubscribe()
            }

            override fun get(wait: Boolean): Array<PresenceMessage> {
                return presence.get(wait)
            }

            override fun enter(data: Any, listener: CompletionListener) {
                return presence.enter(data, listener)
            }

            override fun update(data: Any, listener: CompletionListener) {
                return presence.update(data, listener)
            }

            override fun leave(data: Any, listener: CompletionListener) {
                return presence.leave(data, listener)
            }
        }
    }
}
