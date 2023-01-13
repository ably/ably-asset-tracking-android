package com.ably.tracking.common

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel.MessageListener
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

/**
 * An implementation of [AblySdkFactory] which uses the `ably-java` client library.
 */
class DefaultAblySdkFactory : AblySdkFactory {
    override fun create(clientOptions: ClientOptions): AblySdkRealtime {
        return DefaultAblySdkRealtime(clientOptions)
    }
}

class DefaultAblySdkRealtime
constructor(clientOptions: ClientOptions) : AblySdkRealtime {
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
    }

    class Channels
    constructor(private val channels: AblyRealtime.Channels) : AblySdkRealtime.Channels {
        override fun get(
            channelName: String,
            channelOptions: ChannelOptions?
        ): AblySdkRealtime.Channel {
            return Channel(channels.get(channelName, channelOptions))
        }

        override fun get(channelName: String): AblySdkRealtime.Channel {
            return Channel(channels.get(channelName))
        }

        override fun containsKey(key: Any): Boolean {
            return channels.containsKey(key)
        }

        override fun release(channelName: String) {
            channels.release(channelName)
        }

        override fun entrySet(): Iterable<Map.Entry<String, AblySdkRealtime.Channel>> {
            return channels.entrySet().map { entry ->
                object : Map.Entry<String, AblySdkRealtime.Channel> {
                    override val key = entry.key
                    override val value = Channel(entry.value)
                }
            }
        }
    }

    class Channel
    constructor(private val channel: io.ably.lib.realtime.Channel) : AblySdkRealtime.Channel {
        override val name = channel.name
        override val state: ChannelState
            get() = channel.state
        override val presence = Presence(channel.presence)

        override fun attach(listener: CompletionListener) {
            channel.attach(listener)
        }

        override fun on(listener: ChannelStateListener) {
            channel.on(listener)
        }

        override fun off(listener: ChannelStateListener) {
            channel.off(listener)
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
