package com.ably.tracking.publisher

/**
 * A handler of events relating to the addition, removal and activation of [Trackable] objects for a
 * [Publisher] instance.
 */
interface HooksTrackableSetListener {
    /**
     * A [Trackable] object has been added to the [Publisher]'s set of tracked objects.
     *
     * If the operation adding [trackable] is also making it the [actively][Publisher.active] tracked object
     * then [onActiveTrackableChanged] will subsequently be called.
     *
     * @param trackable The object which has been added to the tracked set.
     */
    fun onTrackableAdded(trackable: Trackable)

    /**
     * A [Trackable] object has been removed from the [Publisher]'s set of tracked objects.
     *
     * If [trackable] was the [actively][Publisher.active] tracked object then [onActiveTrackableChanged] will
     * subsequently be called.
     *
     * @param trackable The object which has been removed from the tracked set.
     */
    fun onTrackableRemoved(trackable: Trackable)

    /**
     * The [actively][Publisher.active] tracked object has changed.
     *
     * @param trackable The object, from the tracked set, which has been activated - or no value if there is
     * no longer an actively tracked object.
     */
    fun onActiveTrackableChanged(trackable: Trackable?)
}
