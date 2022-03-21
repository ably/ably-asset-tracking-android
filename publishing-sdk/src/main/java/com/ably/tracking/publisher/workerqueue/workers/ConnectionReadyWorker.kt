package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ConnectionReadyWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val ably: Ably,
    private val hooks: DefaultCorePublisher.Hooks,
    private val corePublisher: CorePublisher,
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            val presenceData = properties.presenceData.copy()
            return SyncAsyncResult(
                asyncWork = {
                    val result = ably.disconnect(trackable.id, presenceData)
                    ConnectionReadyWorkResult.RemovalRequested(trackable, callbackFunction, result)
                }
            )
        }

        ably.subscribeForChannelStateChange(trackable.id) {
            channelStateChangeListener(it)
        }

        if (!properties.isTracking) {
            corePublisher.startLocationUpdates(properties)
        }

        properties.trackables.add(trackable)
        corePublisher.updateTrackables(properties)
        corePublisher.resolveResolution(trackable, properties)
        hooks.trackables?.onTrackableAdded(trackable)

        val trackableState = properties.trackableStates[trackable.id] ?: TrackableState.Offline()
        val trackableStateFlow = properties.trackableStateFlows[trackable.id] ?: MutableStateFlow(trackableState)
        properties.trackableStateFlows[trackable.id] = trackableStateFlow
        corePublisher.updateTrackableStateFlows(properties)
        properties.trackableStates[trackable.id] = trackableState

        val successResult = Result.success(trackableStateFlow.asStateFlow())
        callbackFunction(successResult)
        properties.duplicateTrackableGuard.finishAddingTrackable(trackable, successResult)

        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
