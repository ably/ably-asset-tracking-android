package com.ably.tracking.subscriber

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

interface PublisherPresence {
    val stateChanges: StateFlow<PublisherPresenceStateChange>

    /**
     * Returns true if we have known publishers present.
     */
    fun hasPresentPublishers(): Boolean

    /**
     * The connection state has entered the offline state, so state must be adjusted accordingly.
     */
    fun connectionOffline()

    /**
     * Process a list of presence messages. It is assumed that the connection has re-entered an online
     * state when this method is called.
     */
    fun processPresenceMessages(messages: List<PresenceMessage>)
}

class DefaultPublisherPresence (
    private val presenceMessageProcessor: PublisherPresenceMessageProcessor,
    private val scope: CoroutineScope
) : PublisherPresence {
    private var lastEmittedStateChange: PublisherPresenceStateChange = PublisherPresenceStateChange(PublisherPresenceState.UNKNOWN, ErrorInformation(
        code = PublisherStateUnknownReasons.SUBSCRIBER_NEVER_ONLINE.value,
        statusCode = 0,
        message = "Subscriber has never been online",
        href = null,
        cause = null
    ), Date().time, listOf())

    private val publisherMap: MutableMap<String, KnownPublisher> = mutableMapOf()

    private val _stateChanges: MutableStateFlow<PublisherPresenceStateChange> = MutableStateFlow(lastEmittedStateChange)

    override val stateChanges: StateFlow<PublisherPresenceStateChange>
        get() = _stateChanges.asStateFlow()

    /**
     * If we receive presence messages, check if we've seen them before. If we're receiving
     * presence messages, we assume that we are in, or transitioning to, the ONLINE state.
     *
     * If we haven't, update our overall publisher state based on the received presence
     * messages.
     *
     * If something material has changed, emit an event.
     */
    override fun processPresenceMessages(messages: List<PresenceMessage>) {

        // Check if there's any message that we haven't seen before
        val newMessages = presenceMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        if (newMessages.isEmpty()) {
            return
        }

        // Update our publisher map
        newMessages.forEach {
            publisherMap[it.memberKey] = KnownPublisher(
                memberKey = it.memberKey,
                clientId = it.clientId,
                connectionId = it.connectionId,
                state = getLastKnownState(it),
                lastSeen = it.timestamp
            )
        }

        /**
         * Unless we're changing overall state or the publishers underlying have changed,
         * then there's nothing to do.
         */
        if (!lastOverallStateWasUnknown() && !publishersHaveChangedSinceLastEmission()) {
            return
        }

        // Emit an event
        // If the individual publishers are present, then their lastSeen time is now.
        val nowTime = Date().time
        emitStateChange(
            PublisherPresenceStateChange(
                when (hasPresentPublishers()) {
                    true -> PublisherPresenceState.PRESENT
                    else -> PublisherPresenceState.ABSENT
                },
                null,
                nowTime,
                publisherMapAsList().map {
                    if (it.state == LastKnownPublisherState.PRESENT) {
                        it.apply {
                            this.lastSeen = nowTime
                        }
                    }

                    it
                }
            )
        )
    }

    /**
     * If the connection transitions to "OFFLINE", then we are entering the
     * overall "unknown" state.
     *
     * For any publisher that was known to be present at the time, set their lastSeen
     * time to now. And emit a state change.
     */
    override fun connectionOffline() {
        publisherMap.forEach {
            it.value.apply {
                if (this.state == LastKnownPublisherState.PRESENT) {
                    this.lastSeen = Date().time
                }
            }
        }

        emitStateChange(
            PublisherPresenceStateChange(
                PublisherPresenceState.UNKNOWN,
                ErrorInformation(
                    code = PublisherStateUnknownReasons.SUBSCRIBER_NOT_ONLINE.value,
                    statusCode = 0,
                    message = "Subscriber is not online",
                    href = null,
                    cause = null
                ),
                Date().time, publisherMapAsList())
        )
    }

    override fun hasPresentPublishers(): Boolean = publisherMap.asSequence().firstOrNull { it.value.state == LastKnownPublisherState.PRESENT } != null

    private fun lastOverallStateWasUnknown(): Boolean = lastEmittedStateChange.state == PublisherPresenceState.UNKNOWN

    /**
     * Checks if publishers have changed since the last event.
     *
     * Returns true if a new publisher has come along, or if one of the publishers
     * states has changed.
     */
    private fun publishersHaveChangedSinceLastEmission(): Boolean {
        if (publisherMap.size != lastEmittedStateChange.publishers.size) {
            return true
        }

        return lastEmittedStateChange.publishers.firstOrNull {
            it.state != publisherMap[it.memberKey]!!.state
        } != null
    }

    private fun getLastKnownState(message: PresenceMessage) = when (message.action) {
        PresenceAction.LEAVE_OR_ABSENT -> LastKnownPublisherState.ABSENT
        else -> LastKnownPublisherState.PRESENT
    }

    private fun publisherMapAsList(): List<KnownPublisher> = publisherMap.toList().map{ it.second }

    private fun emitStateChange(stateChange: PublisherPresenceStateChange) {
        lastEmittedStateChange = stateChange
        scope.launch {
            _stateChanges.emit(stateChange)
        }
    }
}
