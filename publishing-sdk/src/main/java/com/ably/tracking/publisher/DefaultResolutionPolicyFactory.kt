package com.ably.tracking.publisher

import com.ably.tracking.Resolution

class DefaultResolutionPolicyFactory(
    private val defaultResolution: Resolution
) : ResolutionPolicy.Factory {
    override fun createResolutionPolicy(
        hooks: ResolutionPolicy.Hooks,
        methods: ResolutionPolicy.Methods
    ): ResolutionPolicy {
        return Policy(hooks, methods)
    }

    private inner class Policy(
        hooks: ResolutionPolicy.Hooks,
        private val methods: ResolutionPolicy.Methods
    ) : ResolutionPolicy {
        private var thresholdReached = false

        init {
            hooks.trackables(Listener())
        }

        override fun resolve(request: TrackableResolutionRequest): Resolution {
            TODO("Not yet implemented")
        }

        override fun resolve(resolutions: Set<Resolution>): Resolution {
            TODO("Not yet implemented")
        }

        private inner class Listener : ResolutionPolicy.Hooks.TrackableSetListener {
            private val handler = Handler()

            override fun onTrackableAdded(trackable: Trackable) {
                // Implementation intentionally empty. We are not interested in this event.
            }

            override fun onTrackableRemoved(trackable: Trackable) {
                // Implementation intentionally empty. We are not interested in this event.
            }

            override fun onActiveTrackableChanged(trackable: Trackable?) {
                if (null == trackable) {
                    methods.cancelProximityThreshold()
                } else {
                    // TODO Illustrative but will need work!
                    val constraints = trackable.constraints as DefaultResolutionConstraints
                    methods.setProximityThreshold(constraints.proximityThreshold, handler)
                }
            }

            private inner class Handler : ResolutionPolicy.Methods.ProximityHandler {
                override fun onProximityReached(threshold: Proximity) {
                    // modify state for my parent Policy instance
                    thresholdReached = true

                    // ask the Publisher to re-resolve the tracking Resolution, which will result in my parent Policy
                    // instance's implementation of ResolutionPolicy's resolve(Set<ResolutionRequest>) being called
                    // asynchronously at some point in the near future.
                    methods.refresh()
                }

                override fun onProximityCancelled() {
                    // Implementation intentionally empty. We are not interested in this event.
                }
            }
        }
    }
}
