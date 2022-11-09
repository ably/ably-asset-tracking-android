package com.ably.tracking.subscriber

import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceMessage

internal fun processPresenceMessage(
    presenceMessage: PresenceMessage,
    properties: SubscriberProperties,
    subscriberInteractor: SubscriberInteractor,
) {
    when (presenceMessage.action) {
        PresenceAction.PRESENT_OR_ENTER -> {
            if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                subscriberInteractor.updatePublisherPresence(properties, true)
                subscriberInteractor.updateTrackableState(properties)
                subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
            }
        }
        PresenceAction.LEAVE_OR_ABSENT -> {
            if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                subscriberInteractor.updatePublisherPresence(properties, false)
                subscriberInteractor.updateTrackableState(properties)
            }
        }
        PresenceAction.UPDATE -> {
            if (presenceMessage.data.type == ClientTypes.PUBLISHER) {
                subscriberInteractor.updatePublisherResolutionInformation(presenceMessage.data)
            }
        }
    }
}
