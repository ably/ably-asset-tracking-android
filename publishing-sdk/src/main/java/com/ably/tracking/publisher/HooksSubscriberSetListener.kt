package com.ably.tracking.publisher

/**
 * A handler of events relating to the addition or removal of remote [Subscriber]s to a [Publisher] instance.
 */
interface HooksSubscriberSetListener {
    /**
     * A [Subscriber] has subscribed to receive updates for one or more [Trackable] objects from the
     * [Publisher]'s set of tracked objects.
     *
     * @param subscriber The remote entity that subscribed.
     */
    fun onSubscriberAdded(subscriber: Subscriber)

    /**
     * A [Subscriber] has unsubscribed from updates for one or more [Trackable] objects from the [Publisher]'s
     * set of tracked objects.
     *
     * @param subscriber The remote entity that unsubscribed.
     */
    fun onSubscriberRemoved(subscriber: Subscriber)
}
