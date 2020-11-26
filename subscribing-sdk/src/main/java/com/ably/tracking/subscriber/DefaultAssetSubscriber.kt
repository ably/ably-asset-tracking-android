package com.ably.tracking.subscriber

class DefaultAssetSubscriber(
    ablyConfiguration: AblyConfiguration,
    rawLocationUpdatedListener: LocationUpdatedListener,
    enhancedLocationUpdatedListener: LocationUpdatedListener,
    trackingId: String
) : AssetSubscriber {
    override fun stop() {
        TODO()
    }
}
