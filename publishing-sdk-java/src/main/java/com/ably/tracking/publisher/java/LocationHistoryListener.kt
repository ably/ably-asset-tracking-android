package com.ably.tracking.publisher.java

import com.ably.tracking.publisher.LocationHistoryData

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing location history data information.
 */
interface LocationHistoryListener {
    fun onLocationHistory(historyData: LocationHistoryData)
}
