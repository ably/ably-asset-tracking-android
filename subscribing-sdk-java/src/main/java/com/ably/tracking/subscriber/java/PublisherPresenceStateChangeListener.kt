package com.ably.tracking.subscriber.java

import com.ably.tracking.subscriber.PublisherPresenceStateChange

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing the publisher presence state changes.
 */
interface PublisherPresenceStateChangeListener {
    fun onPublisherPresenceStateChanged(publisherPresenceStateChange: PublisherPresenceStateChange)
}
