package com.ably.tracking.subscriber

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

interface PublisherPresence {
    val stateChanges: StateFlow<PublisherPresenceStateChange>

    /**
     * Returns true if the current presence is unknown.
     */
    fun lastStateIsUnknown(): Boolean

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

class DefaultPublisherPresence(
    private val presenceMessageProcessor: PublisherPresenceMessageProcessor,
    private val scope: CoroutineScope
) : PublisherPresence {
    private var lastEmittedStateChange: PublisherPresenceStateChange = PublisherPresenceStateChange(
        PublisherPresenceState.UNKNOWN,
        ErrorInformation(
            code = PublisherStateUnknownReasons.SUBSCRIBER_NEVER_ONLINE.value,
            statusCode = 0,
            message = "Subscriber has never been online",
            href = null,
            cause = null
        ),
        Date().time,
        listOf()
    )

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

        // Check if there's any message that we haven't seen before, update publisher map with new messages
        val newMessages = presenceMessageProcessor.processPresenceMessagesAndGetChanges(messages)
        if (newMessages.isNotEmpty()) {
            newMessages.forEach {
                publisherMap[it.memberKey] = KnownPublisher(
                    memberKey = it.memberKey,
                    clientId = it.clientId,
                    connectionId = it.connectionId,
                    state = getLastKnownState(it),
                    lastSeen = it.timestamp
                )
            }
        }

        /**
         * Unless we're changing overall state or the publishers underlying have changed,
         * then there's nothing to do.
         */
        if (!lastStateIsUnknown() && !publishersHaveChangedSinceLastEmission()) {
            return
        }

        // Update present publishers to "seen now" and emit event
        val now = Date().time
        updatePresentPublishersLastSeenTo(now)

        emitStateChange(
            PublisherPresenceStateChange(
                if (hasPresentPublishers()) PublisherPresenceState.PRESENT else PublisherPresenceState.ABSENT,
                null,
                now,
                publisherMapAsList()
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
        if (lastEmittedStateChange.state == PublisherPresenceState.UNKNOWN) {
            return
        }

        val now = Date().time
        updatePresentPublishersLastSeenTo(now)

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
                now,
                publisherMapAsList()
            )
        )
    }

    override fun hasPresentPublishers(): Boolean = publisherMap.any { it.value.state == LastKnownPublisherState.PRESENT }

    override fun lastStateIsUnknown(): Boolean = lastEmittedStateChange.state == PublisherPresenceState.UNKNOWN

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

    private fun publisherMapAsList(): List<KnownPublisher> = publisherMap.values.toList()

    private fun emitStateChange(stateChange: PublisherPresenceStateChange) {
        lastEmittedStateChange = stateChange
        scope.launch {
            _stateChanges.emit(stateChange)
        }
    }

    private fun updatePresentPublishersLastSeenTo(timestamp: Long) {
        publisherMap.forEach {
            it.value.apply {
                if (this.state == LastKnownPublisherState.PRESENT) {
                    this.lastSeen = timestamp
                }
            }
        }
    }
}
