package com.ably.tracking.publisher

/**
 * Defines the methods to be implemented by proximity handlers.
 */
interface ResolutionPolicyProximityHandler {
    /**
     * The desired proximity has been reached.
     *
     * @param threshold The threshold which was supplied when this handler was registered.
     */
    fun onProximityReached(threshold: Proximity)

    /**
     * This handler has been cancelled, either explicitly using [cancelProximityThreshold], or implicitly
     * because a new handler has taken its place for the associated [Publisher].
     */
    fun onProximityCancelled()
}
