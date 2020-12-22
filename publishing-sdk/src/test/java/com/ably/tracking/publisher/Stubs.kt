package com.ably.tracking.publisher

class HooksStub : ResolutionPolicy.Hooks {
    var trackableSetListener: ResolutionPolicy.Hooks.TrackableSetListener? = null
    var subscriberSetListener: ResolutionPolicy.Hooks.SubscriberSetListener? = null
    override fun trackables(listener: ResolutionPolicy.Hooks.TrackableSetListener) {
        trackableSetListener = listener
    }

    override fun subscribers(listener: ResolutionPolicy.Hooks.SubscriberSetListener) {
        subscriberSetListener = listener
    }
}

class MethodsStub : ResolutionPolicy.Methods {
    private var threshold: Proximity? = null
    private var proximityHandler: ResolutionPolicy.Methods.ProximityHandler? = null
    override fun refresh() = Unit

    override fun setProximityThreshold(threshold: Proximity, handler: ResolutionPolicy.Methods.ProximityHandler) {
        this.threshold = threshold
        proximityHandler = handler
    }

    override fun cancelProximityThreshold() = Unit

    fun onProximityReached() {
        threshold?.let { proximityHandler?.onProximityReached(it) }
    }
}
