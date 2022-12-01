package com.ably.tracking.publisher

/**
 * Defines the methods which can be called by a resolution policy when it is created.
 *
 * Methods on this interface may only be called from within implementations of
 * [createResolutionPolicy][Factory.createResolutionPolicy].
 */
interface ResolutionPolicyHooks {

    /**
     * Register a handler for the addition, removal and activation of [Trackable] objects for the [Publisher]
     * instance whose [creation][Publisher.Builder.start] caused
     * [createResolutionPolicy][Factory.createResolutionPolicy] to be called.
     *
     * This method should only be called once within the scope of creation of a single publisher's resolution
     * policy. Subsequent calls to this method will replace the previous handler.
     *
     * @param listener The handler, which may be called multiple times during the lifespan of the publisher.
     */
    fun trackables(listener: HooksTrackableSetListener)

    /**
     * Register a handler for the addition and removal of remote [Subscriber]s to the [Publisher] instance whose
     * [creation][Publisher.Builder.start] caused [createResolutionPolicy][Factory.createResolutionPolicy] to be
     * called.
     *
     * This method should only be called once within the scope of creation of a single publisher's resolution
     * policy. Subsequent calls to this method will replace the previous handler.
     *
     * @param listener The handler, which may be called multiple times during the lifespan of the publisher.
     */
    fun subscribers(listener: HooksSubscriberSetListener)
}
