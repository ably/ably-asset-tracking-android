fun main() {
    TODO()
}



enum class BatteryThresholdState {
    BELOW,
    ABOVE
}

enum class SubscribersPresenceState {
    NOT_PRESENT,
    PRESENT
}

enum class ProximityThresholdState {
    BELOW,
    ABOVE
}

class ResolutionsParametersState(
    val proximityState: ProximityThresholdState,
    val subscribersPresenceState: SubscribersPresenceState
)

class Accuracy
typealias Proximity = Float
typealias BatteryLevel = Float

data class Resolution(
    val accuracy: Accuracy,
    val interval: Long,
    val displacement: Double
)

data class Subscriber (
    var requestedResolution: Resolution?
)

fun winningResolution(resolutions: List<Resolution>): Resolution {
    TODO()
}

fun applyBatteryMultiplier(batteryMultiplier: Float, resolution: Resolution): Resolution {
    TODO()
}

// How application developer can supply a set of parameters to trackable. This set of parameters, for example, can be obtained from a remote location (i.e. server)
data class Trackable(
    val defaultResolution: Resolution,
    val resolutionParameters: HashMap<ResolutionsParametersState, Resolution>,
    val proximityTreshold: Proximity,
    val batteryThreshold: BatteryLevel,
    val batteryMultiplier: Float // multiplier to be applied to interval when battery level is below the threshold
) {
    fun proximityToDestination(): Proximity {
        TODO()
    }
    fun isActive(): Boolean {
        TODO()
    }
    fun subscribers(): List<Subscriber> {
        TODO()
    }
}


fun computeNetworkingResolution(trackable: Trackable, deviceBatteryLevel: BatteryLevel): Resolution {
    // identify the current state

    val proximityState: ProximityThresholdState = if (trackable.proximityToDestination() < trackable.proximityTreshold)
        ProximityThresholdState.BELOW
    else
        ProximityThresholdState.ABOVE

    val subscribers = trackable.subscribers()
    val subscribersPresenceState = if (subscribers.isEmpty())
        SubscribersPresenceState.NOT_PRESENT
    else
        SubscribersPresenceState.PRESENT

    val batteryState = if (deviceBatteryLevel < trackable.batteryThreshold)
        BatteryThresholdState.BELOW
    else
        BatteryThresholdState.ABOVE

    val state = ResolutionsParametersState(proximityState, subscribersPresenceState)

    var intermediateResolution: Resolution

    // First lets asses proximity, and update `intermediateResolution` with candidate for final resolution
    intermediateResolution = if (proximityState == ProximityThresholdState.ABOVE)
        trackable.resolutionParameters[state] ?:
        trackable.defaultResolution
    else {
        trackable.resolutionParameters[state] ?:
        trackable.resolutionParameters[ResolutionsParametersState(ProximityThresholdState.ABOVE, SubscribersPresenceState.NOT_PRESENT)] ?:
        trackable.defaultResolution
    }

    // If there are subscribers, find the lowest resolutions among the requested ones and the candidate we have identified
    if (subscribersPresenceState == SubscribersPresenceState.PRESENT) {
        val requestedResolutions = trackable.subscribers().map { it.requestedResolution }.toMutableList()
        requestedResolutions.add(intermediateResolution)
        intermediateResolution = winningResolution(requestedResolutions.filterNotNull()) // as I am adding Resolution above
    }

    // Apply low battery multiplier
    return if (batteryState == BatteryThresholdState.BELOW)
        applyBatteryMultiplier(trackable.batteryMultiplier, intermediateResolution)
    else
        intermediateResolution
}

fun computeLocationSamplingResolution(resolutions: List<Resolution>): Resolution {
    return winningResolution(resolutions)
}



/*
Assumptions made:
* this policy can work even if user supplies at least one resolution (default) per trackable. The logic will still take requests from subscribers into account.
* beyond the default one user can supply additional resolutions for different states, that will be used if present
* user provides thresholds for proximity and battery per trackable. Setting those at 0 (or another special state) will effectively disable taking those factors into account
* some subscribers may not request specific resolution
* if there is conflicting resolution request from subscribers and resolution supplied by user - lower resolution wins
* The fallback logic is hardcoded, but should be logical in most cases
* logic around battery level is extremely simple - just a multiplier
* user cannot supply multiple "tiers" for battery or proximity
* when battery is low, the multiplier is applied even if subscribers requested better resolution

Assumptions addressed and removed from above:
* the fallback logic right now is extremely simple - it is always to "defaultResolution", but should be "smarter"
* right now the logic above assumes that user provides separate Resolution parameters for having one and multiple subscribers, we probably will not need that
 */
