package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.PresenceData
import com.ably.tracking.publisher.AddTrackableCallbackFunction
import com.ably.tracking.publisher.AddTrackableResult
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.guards.DuplicateTrackableGuard
import com.ably.tracking.publisher.guards.TrackableRemovalGuard
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeProperties(
    override val duplicateTrackableGuard: DuplicateTrackableGuard,
    override val trackableRemovalGuard: TrackableRemovalGuard
) : PublisherProperties {
    override val trackables: MutableSet<Trackable> = mutableSetOf()
    override val trackableStateFlows: MutableMap<String, MutableStateFlow<TrackableState>> = mutableMapOf()
    override var presenceData: PresenceData = PresenceData("properties")
}

internal class FakeDuplicateGuard(private val currentlyAdding: Boolean) : DuplicateTrackableGuard {
    private val trackables: MutableSet<Trackable> = mutableSetOf()

    override fun startAddingTrackable(trackable: Trackable) {
        trackables.add(trackable)
    }

    override fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>) {
        // does not need implementing for now
    }

    override fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean {
        return currentlyAdding
    }

    override fun saveDuplicateAddHandler(trackable: Trackable, callbackFunction: AddTrackableCallbackFunction) {
        // does not need implementing
    }

    override fun clear(trackable: Trackable) {
        trackables.remove(trackable)
    }

    override fun clearAll() {
        trackables.clear()
    }
}
