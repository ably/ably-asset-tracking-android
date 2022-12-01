package com.ably.tracking.publisher

class HooksStub : ResolutionPolicyHooks {
    var trackableSetListener: HooksTrackableSetListener? = null
    var subscriberSetListener: HooksSubscriberSetListener? = null
    override fun trackables(listener: HooksTrackableSetListener) {
        trackableSetListener = listener
    }

    override fun subscribers(listener: HooksSubscriberSetListener) {
        subscriberSetListener = listener
    }
}

class MethodsStub : ResolutionPolicyMethods {
    private var threshold: Proximity? = null
    private var proximityHandler: ResolutionPolicyProximityHandler? = null
    override fun refresh() = Unit

    override fun setProximityThreshold(threshold: Proximity, handler: ResolutionPolicyProximityHandler) {
        this.threshold = threshold
        proximityHandler = handler
    }

    override fun cancelProximityThreshold() = Unit

    fun onProximityReached() {
        threshold?.let { proximityHandler?.onProximityReached(it) }
    }
}
