package com.ably.tracking.common

import io.ably.lib.realtime.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AblyChannelsGuard {
    /**
     * Channels that have successfully completed the connect operation.
     */
    private val channels: MutableMap<String, Channel> = mutableMapOf()

    /**
     * Trackable IDs of channels that have started the connect operation and are waiting for its result.
     */
    private val connectingChannelTrackableIds: MutableSet<String> = mutableSetOf()

    /**
     * Mutex making sure the [channels] and [connectingChannelTrackableIds] are accessed in a synchronized manner.
     */
    private val channelsMutex = Mutex()

    fun getChannel(trackableId: String): Channel? = channels[trackableId]

    fun getChannelTrackableIds(): Set<String> = channels.keys

    fun isChannelCreated(trackableId: String): Boolean = channels.containsKey(trackableId)

    suspend fun startConnectingChannel(trackableId: String, onConnectingFirstChannel: suspend () -> Unit) {
        channelsMutex.withLock {
            val isCreatingFirstChannel = channels.isEmpty() && connectingChannelTrackableIds.isEmpty()
            if (isCreatingFirstChannel) {
                onConnectingFirstChannel()
            }
            connectingChannelTrackableIds.add(trackableId)
        }
    }

    suspend fun channelConnected(trackableId: String, channel: Channel) {
        channelsMutex.withLock {
            channels[trackableId] = channel
            connectingChannelTrackableIds.remove(trackableId)
        }
    }

    suspend fun channelConnectFailed(trackableId: String, onNoConnectedChannels: suspend () -> Unit) {
        channelsMutex.withLock {
            connectingChannelTrackableIds.remove(trackableId)
            if (hasNoConnectedOrConnectingChannels) {
                onNoConnectedChannels()
            }
        }
    }

    suspend fun channelDisconnected(trackableId: String, onNoConnectedChannels: suspend () -> Unit) {
        channelsMutex.withLock {
            channels.remove(trackableId)
            if (hasNoConnectedOrConnectingChannels) {
                onNoConnectedChannels()
            }
        }
    }

    private val hasNoConnectedOrConnectingChannels
        get() = connectingChannelTrackableIds.isEmpty() && channels.isEmpty()
}
