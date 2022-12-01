package com.ably.tracking.publisher

/**
 * Defines the methods which can be called by a resolution policy at any time.
 */
interface ResolutionPolicyMethods {

    /**
     * Causes the current tracking [Resolution] to be evaluated again by its associated [Publisher] instance.
     *
     * The [ResolutionPolicy] instance which was provided with these [Methods] will be consulted again as soon as
     * possible after this method returns.
     */
    fun refresh()

    /**
     * Registers a handler to be called when a given proximity to the destination of the active [Trackable] object
     * has been reached.
     *
     * Proximity is considered reached when it is at or nearer the destination than the [threshold] specifies.
     *
     * A [Publisher] instance can only have one proximity threshold handler active at any one time. If there is
     * already a registered proximity handler then it will be cancelled and replaced.
     *
     * The supplied [handler] will only be called once, either:
     * - with [onProximityReached][ProximityHandler.onProximityReached] when proximity has been reached;
     * - or with [onProximityCancelled][ProximityHandler.onProximityCancelled] when cancelled (either
     * [explicitly][cancelProximityThreshold] or implicitly because it was replaced by a subsequent call to this
     * method)
     *
     * @param threshold The threshold at which to call the handler.
     * @param handler The handler whose [onProximityReached][ProximityHandler.onProximityReached] method is to be
     * called when this threshold proximity has been reached.
     */
    fun setProximityThreshold(threshold: Proximity, handler: ResolutionPolicyProximityHandler)

    /**
     * Removes the currently registered proximity handler, if there is one. Otherwise does nothing.
     */
    fun cancelProximityThreshold()
}
