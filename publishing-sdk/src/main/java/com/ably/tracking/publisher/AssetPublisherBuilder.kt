package com.ably.tracking.publisher

import android.content.Context

internal data class AssetPublisherBuilder(
    val ablyConfiguration: AblyConfiguration? = null,
    val mapConfiguration: MapConfiguration? = null,
    val logConfiguration: LogConfiguration? = null,
    val batteryConfiguration: BatteryConfiguration? = null,
    val assetMetadataJson: String? = null,
    val tripMetadataJson: String? = null,
    val locationUpdatedListener: LocationUpdatedListener? = null,
    val androidContext: Context? = null,
    val trackingId: String? = null,
    val destination: String? = null,
    val vehicleType: String? = null
) : AssetPublisher.Builder {
    override fun ablyConfig(configuration: AblyConfiguration): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun mapConfig(configuration: MapConfiguration): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun logConfig(configuration: LogConfiguration): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun batteryConfig(configuration: BatteryConfiguration): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun assetMetadataJson(metadataJsonString: String): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun tripMetadataJson(metadataJsonString: String): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun locationUpdatedListener(listener: LocationUpdatedListener): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun androidContext(context: Context): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun delivery(
        trackingId: String,
        destination: String,
        vehicleType: String
    ): AssetPublisher.Builder {
        TODO("Not yet implemented")
    }

    override fun start(): AssetPublisher {
        TODO("Not yet implemented")
    }
}
