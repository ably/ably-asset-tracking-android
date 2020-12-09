fun main() {
    println("Hello World")
}

enum class BatteryThresholdState {
    BELOW,
    ABOVE
}

enum class SubscribersPresenceState {
    NONE,
    ONE,
    MULTIPLE
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
    val batteryState: BatteryThresholdState
    val proximityState: ProximityThresholdState
    val subscribersPresenceState: SubscribersPresenceState

    batteryState = if (deviceBatteryLevel < trackable.batteryThreshold)
        BatteryThresholdState.BELOW
    else
        BatteryThresholdState.ABOVE

    proximityState = if (trackable.proximityToDestination() < trackable.proximityTreshold)
        ProximityThresholdState.BELOW
    else
        ProximityThresholdState.ABOVE


    val subscribers = trackable.subscribers()
    subscribersPresenceState = when (subscribers.size) {
        0 -> SubscribersPresenceState.NONE
        1 -> SubscribersPresenceState.ONE
        else -> SubscribersPresenceState.MULTIPLE
    }

    val currentState = Triple(batteryState, proximityState, subscribersPresenceState)

    var intermediateResolution: Resolution? = null

    // Probably really bad design for production code as creates throwaway objects, but I think it will be good for
    // explaining the idea
    when (currentState) {
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.ABOVE, SubscribersPresenceState.NONE) -> {
            intermediateResolution = trackable.resolutionParameters[currentState]
        }
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.ABOVE, SubscribersPresenceState.ONE) -> {
            val requestedResolution = trackable.subscribers().last().requestedResolution
            intermediateResolution = if (requestedResolution != null)
                requestedResolution
            else
                trackable.resolutionParameters[currentState]
        }
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.ABOVE, SubscribersPresenceState.MULTIPLE) -> {
            val requestedResolutions = trackable.subscribers().map { it.requestedResolution }
            val requestedResolution = lowestResolution(requestedResolutions)
            intermediateResolution = if (requestedResolution != null)
                requestedResolution
            else
                trackable.resolutionParameters[currentState]
        }
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.BELOW, SubscribersPresenceState.NONE) -> {
            intermediateResolution = trackable.resolutionParameters[currentState]
        }
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.BELOW, SubscribersPresenceState.ONE) -> {
            val requestedResolution = trackable.subscribers().last().requestedResolution
            intermediateResolution = lowestResolution(listOf(requestedResolution, trackable.resolutionParameters[currentState]))
        }
        Triple(BatteryThresholdState.ABOVE, ProximityThresholdState.BELOW, SubscribersPresenceState.MULTIPLE) -> {
            var requestedResolutions = trackable.subscribers().map { it.requestedResolution } .toMutableList()
            requestedResolutions.add(trackable.resolutionParameters[currentState])
            intermediateResolution = lowestResolution(requestedResolutions)
        }
    }

    intermediateResolution =  if (intermediateResolution != null)
        intermediateResolution!!
    else
        defaultResolution

    if (batteryState == BatteryThresholdState.BELOW)
        return applyBatteryMultiplier(trackable.batteryMultiplier, intermediateResolution!!)
}

/*
Assumptions made:
* user supplies parameters for all states
* some subscribers may not request specific resolution
* if there is conflicting resolution request from subscribers and proximity threshold resolution supplied by user - lower resolution wins
* right now the logic above assumes that user provides separate Resolution parameters for having one and multiple subscribers, we probably will not need that
* the fallback logic right now is extremely simple - it is always to "defaultResolution", but should be "smarter"
 */
