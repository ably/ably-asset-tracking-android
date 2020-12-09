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

data class Trackable(
    val resolutionParameters:
        HashMap<Triple<BatteryThresholdState, ProximityThresholdState, SubscribersPresenceState>, Resolution>,
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

fun lowestResolution(resolutions: List<Resolution?>): Resolution? {
    TODO()
}

fun applyBatteryMultiplier(batteryMultiplier: Float, resolution: Resolution): Resolution {
    TODO()
}



fun computeResolution(trackable: Trackable, deviceBatteryLevel: BatteryLevel, defaultResolution: Resolution): Resolution {
    // identify the current state
    val batteryState: BatteryThresholdState = if (deviceBatteryLevel < trackable.batteryThreshold)
        BatteryThresholdState.BELOW
    else
        BatteryThresholdState.ABOVE

    val proximityState: ProximityThresholdState = if (trackable.proximityToDestination() < trackable.proximityTreshold)
        ProximityThresholdState.BELOW
    else
        ProximityThresholdState.ABOVE

    val subscribers = trackable.subscribers()
    val subscribersPresenceState = if (subscribers.isEmpty())
            SubscribersPresenceState.NOT_PRESENT
        else
            SubscribersPresenceState.PRESENT

    val currentState = Triple(batteryState, proximityState, subscribersPresenceState)

    var intermediateResolution: Resolution? = null

    if (subscribersPresenceState == SubscribersPresenceState.NOT_PRESENT) {
        intermediateResolution = trackable.resolutionParameters[currentState]
    } else {
        val requestedResolutions = trackable.subscribers().map { it.requestedResolution }.toMutableList()
        requestedResolutions.add(trackable.resolutionParameters[currentState])
        intermediateResolution = lowestResolution(requestedResolutions)
    }

    intermediateResolution = intermediateResolution ?: defaultResolution

    return if (batteryState == BatteryThresholdState.BELOW)
        applyBatteryMultiplier(trackable.batteryMultiplier, intermediateResolution)
    else
        intermediateResolution
}

/*
Assumptions made:
* user supplies parameters for all states
* some subscribers may not request specific resolution
* if there is conflicting resolution request from subscribers and proximity threshold resolution supplied by user - lower resolution wins
* the fallback logic right now is extremely simple - it is always to "defaultResolution", but should be "smarter"

Assumptions addressed and removed from above:
* right now the logic above assumes that user provides separate Resolution parameters for having one and multiple subscribers, we probably will not need that
 */
