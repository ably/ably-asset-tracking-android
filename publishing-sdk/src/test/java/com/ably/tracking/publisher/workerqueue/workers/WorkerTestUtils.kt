package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.LocationsPublishingState
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

fun MutableList<suspend () -> Unit>.appendWork(): (suspend () -> Unit) -> Unit =
    { asyncWork ->
        add(asyncWork)
    }

suspend fun MutableList<suspend () -> Unit>.executeAll() {
    forEach { it.invoke() }
}

internal fun MutableList<WorkerSpecification>.appendSpecification(): (WorkerSpecification) -> Unit =
    { workSpecification ->
        add(workSpecification)
    }

internal fun createPublisherProperties(
    routingProfile: RoutingProfile = RoutingProfile.DRIVING,
    locationEngineResolution: Resolution = Resolution(Accuracy.BALANCED, 1000L, 100.0),
    isLocationEngineResolutionConstant: Boolean = false,
    areRawLocationsEnabled: Boolean? = null,
    onActiveTrackableUpdated: (Trackable?) -> Unit = {},
    onRoutingProfileUpdated: (RoutingProfile) -> Unit = {}
): PublisherProperties = PublisherProperties(
    routingProfile,
    locationEngineResolution,
    isLocationEngineResolutionConstant,
    areRawLocationsEnabled,
    onActiveTrackableUpdated,
    onRoutingProfileUpdated
)

internal fun PublisherProperties.insertResolutions(resolutionSet: Set<Resolution>): PublisherProperties {
    resolutionSet
        .mapIndexed { index, resolution -> index.toString() to resolution }
        .forEach { resolutions[it.first] = it.second }
    return this
}

internal fun <T : Any> LocationsPublishingState<T>.maxOutRetryCount(trackableId: String) {
    while (shouldRetryPublishing(trackableId)) {
        incrementRetryCount(trackableId)
    }
}

