package com.ably.tracking.subscriber

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceMessage

/**
 * An interface for filtering out presence messages to ones we care about.
 */
interface PublisherPresenceMessageProcessor {
    /**
     * Given a list of presence messages, process them and return the last presence message for
     * any given member that we haven't yet processed.
     */
    fun processPresenceMessagesAndGetChanges(messages: List<PresenceMessage>): List<PresenceMessage>
}

class DefaultPublisherPresenceMessageProcessor: PublisherPresenceMessageProcessor {

    // Stores the last applicable presence message associated with this member
    private val memberMap: MutableMap<String, PresenceMessage> = mutableMapOf()

    override fun processPresenceMessagesAndGetChanges(messages: List<PresenceMessage>): List<PresenceMessage> {

        // Create a list of messages that we've actually processed
        val newMessages = mutableMapOf<String, PresenceMessage>()

        // If the message is about a publisher and is newer than the last one we have for that publisher, process it
        for (presenceMessage: PresenceMessage in messages) {
            if (presenceMessage.data.type != ClientTypes.PUBLISHER) {
                continue
            }

            if (
                !memberMap.contains(presenceMessage.memberKey) ||
                presenceMessage.isNewerThan(memberMap[presenceMessage.memberKey]!!)
            ) {
                memberMap[presenceMessage.memberKey] = presenceMessage
                newMessages[presenceMessage.memberKey] = presenceMessage
            }
        }

        return newMessages.toList().map { it.second }
    }
}
