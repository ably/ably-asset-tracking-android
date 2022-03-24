package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.ConnectionReadyWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
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
            return SyncAsyncResult(asyncWork = { onTrackableRemovalRequested(presenceData) })
        }

        subscribeForChannelStateChanges()
        startLocationUpdates(properties)
        addTrackableToPublisher(properties)
        val trackableState = properties.trackableStates[trackable.id] ?: TrackableState.Offline()
        val trackableStateFlow = properties.trackableStateFlows[trackable.id] ?: MutableStateFlow(trackableState)
        updateTrackableState(properties, trackableState, trackableStateFlow)
        notifyAddOperationFinished(properties, trackableStateFlow)

        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    private suspend fun onTrackableRemovalRequested(presenceData: PresenceData): WorkResult {
        val result = ably.disconnect(trackable.id, presenceData)
        return ConnectionReadyWorkResult.RemovalRequested(trackable, callbackFunction, result)
    }

    private fun subscribeForChannelStateChanges() {
        ably.subscribeForChannelStateChange(trackable.id) {
            channelStateChangeListener(it)
        }
    }

    private fun startLocationUpdates(properties: PublisherProperties) {
        if (!properties.isTracking) {
            corePublisher.startLocationUpdates(properties)
        }
    }

    private fun addTrackableToPublisher(properties: PublisherProperties) {
        properties.trackables.add(trackable)
        corePublisher.updateTrackables(properties)
        corePublisher.resolveResolution(trackable, properties)
        hooks.trackables?.onTrackableAdded(trackable)
    }

    private fun updateTrackableState(
        properties: PublisherProperties,
        trackableState: TrackableState,
        trackableStateFlow: MutableStateFlow<TrackableState>
    ) {
        properties.trackableStateFlows[trackable.id] = trackableStateFlow
        corePublisher.updateTrackableStateFlows(properties)
        properties.trackableStates[trackable.id] = trackableState
    }

    private fun notifyAddOperationFinished(
        properties: PublisherProperties,
        trackableStateFlow: MutableStateFlow<TrackableState>
    ) {
        val successResult = Result.success(trackableStateFlow.asStateFlow())
        callbackFunction(successResult)
        properties.duplicateTrackableGuard.finishAddingTrackable(trackable, successResult)
    }
}
