package com.ably.tracking.publisher

import android.content.Context
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.getLevel
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

private class DefaultResolutionPolicy(
    hooks: ResolutionPolicy.Hooks,
    methods: ResolutionPolicy.Methods,
    private val defaultResolution: Resolution,
    private val batteryDataProvider: BatteryDataProvider
) : ResolutionPolicy {
    private val proximityHandler =
        DefaultProximityHandler(methods, { proximityThresholdReached = true }, { proximityThresholdReached = false })
    private val subscriberSetListener = DefaultSubscriberSetListener()
    private var proximityThresholdReached = false

    init {
        hooks.trackables(DefaultTrackableSetListener(methods, proximityHandler))
        hooks.subscribers(subscriberSetListener)
    }

    override fun resolve(resolutions: Set<Resolution>): Resolution =
        resolve(TrackableResolutionRequest(null, resolutions))

    override fun resolve(request: TrackableResolutionRequest): Resolution =
        if (request.constraints != null) {
            when (request.constraints) {
                is DefaultResolutionConstraints -> resolveWithDefaultResolutionConstraints(request)
            }
        } else {
            resolveWithoutConstraints(request)
        }

    private fun resolveWithoutConstraints(request: TrackableResolutionRequest): Resolution =
        if (request.remoteRequests.isEmpty()) defaultResolution else createFinalResolution(request.remoteRequests)

    /**
     * We're expecting that [ResolutionConstraints] from [request] is not null and is of type [DefaultResolutionConstraints].
     */
    private fun resolveWithDefaultResolutionConstraints(request: TrackableResolutionRequest): Resolution {
        val trackableConstraints = request.constraints as DefaultResolutionConstraints
        val resolutionFromTrackable = trackableConstraints.resolutions.getResolution(
            proximityThresholdReached,
            subscriberSetListener.hasSubscribers()
        )
        val allResolutions = mutableSetOf<Resolution>().apply {
            add(resolutionFromTrackable)
            request.remoteRequests.let { if (it.isNotEmpty()) addAll(it) }
        }
        val finalResolution = if (allResolutions.isEmpty()) defaultResolution else createFinalResolution(allResolutions)
        return adjustResolutionToBatteryLevel(finalResolution, trackableConstraints)
    }

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

    private fun higher(a: Accuracy, b: Accuracy): Accuracy = if (a.getLevel() > b.getLevel()) a else b
}

private class DefaultSubscriberSetListener : ResolutionPolicy.Hooks.SubscriberSetListener {
    private val subscriberSet = mutableSetOf<Subscriber>()
    override fun onSubscriberAdded(subscriber: Subscriber) {
        subscriberSet.add(subscriber)
    }

    override fun onSubscriberRemoved(subscriber: Subscriber) {
        subscriberSet.remove(subscriber)
    }

    fun hasSubscribers() = subscriberSet.isNotEmpty()
}

private class DefaultTrackableSetListener(
    private val methods: ResolutionPolicy.Methods,
    private val proximityHandler: DefaultProximityHandler
) :
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

private class DefaultProximityHandler(
    private val methods: ResolutionPolicy.Methods,
    private val proximityReachedCallback: () -> Unit,
    private val proximityCancelledCallback: () -> Unit
) :
    ResolutionPolicy.Methods.ProximityHandler {
    override fun onProximityReached(threshold: Proximity) {
        proximityReachedCallback()
        methods.refresh()
    }

    override fun onProximityCancelled() {
        proximityCancelledCallback()
    }
}
