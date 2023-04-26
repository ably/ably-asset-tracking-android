package com.ably.tracking.test.android.common

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.PresenceMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Wait until Subscriber joins the channel for given trackableId with given timeout.
 */
suspend fun AblyRealtime.awaitSubscriberPresent(trackableId: String, timeout: Long) {
    withTimeout(timeout) {
        while (!subscriberIsPresent(trackableId)) {
            delay(200)
        }
    }
}

/**
 * Perform a request to the Ably API to get a snapshot of the current presence for the channel,
 * and check to see if the Subscriber's clientId is present in that snapshot.
 */
fun AblyRealtime.subscriberIsPresent(trackableId: String) =
    channels
        .get("tracking:$trackableId")
        .presence
        .get(true)
        .any {
            it.clientId == SUBSCRIBER_CLIENT_ID &&
                it.action == PresenceMessage.Action.present
        }
