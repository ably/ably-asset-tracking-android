package com.ably.tracking.publisher

import android.content.Context
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import kotlin.math.min

class DefaultResolutionPolicyFactory(
    private val defaultResolution: Resolution,
    private val context: Context
) : ResolutionPolicy.Factory {
    override fun createResolutionPolicy(
        hooks: ResolutionPolicy.Hooks,
        methods: ResolutionPolicy.Methods
    ): ResolutionPolicy {
        return DefaultResolutionPolicy(hooks, methods, defaultResolution, DefaultBatteryDataProvider(context))
    }
}

internal class DefaultResolutionPolicy(
    hooks: ResolutionPolicy.Hooks,
    private val methods: ResolutionPolicy.Methods,
    private val defaultResolution: Resolution,
    private val batteryDataProvider: BatteryDataProvider
) : ResolutionPolicy {
    private val proximityHandler = ProximityHandler()
    private val subscriberSetListener = SubscriberSetListener()
    private var proximityThresholdReached = false

    init {
        hooks.trackables(DefaultTrackableSetListener())
        hooks.subscribers(subscriberSetListener)
    }

    override fun resolve(resolutions: Set<Resolution>): Resolution =
        resolveFromRequests(resolutions)

    override fun resolve(request: TrackableResolutionRequest): Resolution =
        request.trackable.constraints.let { constraints ->
            when (constraints) {
                null -> resolveFromRequests(request.remoteRequests)
                is DefaultResolutionConstraints -> {
                    val resolutionFromTrackable = getResolutionFromTrackable(constraints, request.trackable)
                    val allResolutions = mutableSetOf<Resolution>().apply {
                        add(resolutionFromTrackable)
                        request.remoteRequests.let { if (it.isNotEmpty()) addAll(it) }
                    }
                    val finalResolution =
                        if (allResolutions.isEmpty()) defaultResolution else createFinalResolution(allResolutions)
                    return adjustResolutionToBatteryLevel(finalResolution, constraints)
                }
            }
        }

    private fun getResolutionFromTrackable(
        resolutionConstraints: DefaultResolutionConstraints,
        trackable: Trackable
    ): Resolution {
        val hasSubscribers = subscriberSetListener.hasSubscribers(trackable)

        return if (proximityThresholdReached)
            resolutionConstraints.resolutions.getResolution(isNear = true, hasSubscriber = hasSubscribers)
                ?: defaultResolution
        else {
            resolutionConstraints.resolutions.getResolution(isNear = false, hasSubscriber = hasSubscribers)
                ?: resolutionConstraints.resolutions.getResolution(isNear = true, hasSubscriber = false)
                ?: defaultResolution
        }
    }

    private fun resolveFromRequests(requests: Set<Resolution>): Resolution =
        if (requests.isEmpty()) defaultResolution else createFinalResolution(requests)

    private fun adjustResolutionToBatteryLevel(
        resolution: Resolution,
        constraints: DefaultResolutionConstraints
    ): Resolution =
        batteryDataProvider.getCurrentBatteryPercentage().let { batteryPercentage ->
            if (batteryPercentage != null && batteryPercentage < constraints.batteryLevelThreshold) {
                val newInterval = resolution.desiredInterval * constraints.lowBatteryMultiplier
                resolution.copy(desiredInterval = newInterval.toLong())
            } else {
                resolution
            }
        }

    private fun createFinalResolution(resolutions: Set<Resolution>): Resolution {
        var accuracy = Accuracy.MINIMUM
        var desiredInterval = Long.MAX_VALUE
        var minimumDisplacement = Double.MAX_VALUE
        resolutions.forEach {
            accuracy = higher(accuracy, it.accuracy)
            desiredInterval = min(desiredInterval, it.desiredInterval)
            minimumDisplacement = min(minimumDisplacement, it.minimumDisplacement)
        }
        return Resolution(accuracy, desiredInterval, minimumDisplacement)
    }

    private fun higher(a: Accuracy, b: Accuracy): Accuracy = if (a.level > b.level) a else b

    private inner class SubscriberSetListener : ResolutionPolicy.Hooks.SubscriberSetListener {
        private val subscriberSet = mutableSetOf<Subscriber>()
        override fun onSubscriberAdded(subscriber: Subscriber) {
            subscriberSet.add(subscriber)
        }

        override fun onSubscriberRemoved(subscriber: Subscriber) {
            subscriberSet.remove(subscriber)
        }

        fun hasSubscribers(trackable: Trackable) = subscriberSet.any { it.trackable == trackable }
    }

    private inner class DefaultTrackableSetListener :
        ResolutionPolicy.Hooks.TrackableSetListener {
        private val trackableSet = mutableSetOf<Trackable>()
        private var activeTrackable: Trackable? = null
        override fun onTrackableAdded(trackable: Trackable) {
            trackableSet.add(trackable)
        }

        override fun onTrackableRemoved(trackable: Trackable) {
            trackableSet.remove(trackable)
        }

        override fun onActiveTrackableChanged(trackable: Trackable?) {
            if (trackable == null) {
                activeTrackable = null
                methods.cancelProximityThreshold()
            } else {
                activeTrackable = trackable
                trackable.constraints?.let {
                    val constraints = it as DefaultResolutionConstraints
                    methods.setProximityThreshold(constraints.proximityThreshold, proximityHandler)
                }
            }
        }
    }

    private inner class ProximityHandler :
        ResolutionPolicy.Methods.ProximityHandler {
        override fun onProximityReached(threshold: Proximity) {
            proximityThresholdReached = true
            methods.refresh()
        }

        override fun onProximityCancelled() {
            proximityThresholdReached = false
        }
    }
}
