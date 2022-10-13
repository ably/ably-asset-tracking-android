package com.ably.tracking.publisher.workerqueue.results

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal typealias AsyncWork = (suspend () -> WorkResult)

/**
 * This sealed class represents a base result from concrete [Worker] instances after they finish doing their work.
 * It's also intended to be returned by [AsyncWork] coroutines.
 */
internal sealed class WorkResult

/**
 * A special [WorkResult] that contains an optional [syncWorkResult] and [asyncWork]
 *
 * @param syncWorkResult any work result that has resulted from synchronous work. This work result is expected to be
 * returned and processed in a blocking fashion.
 * @param asyncWork is any work that is not immediately executed but returned to caller for background execution.
 *
 * [asyncWork] is a suspending work and it is intended to be launched in a different coroutine. However there is
 * nothing that prevents callers from executing this in the same coroutine. If any use case like this arise, you
 * should document the reason of such usage.
 */
internal data class SyncAsyncResult(
    val syncWorkResult: WorkResult? = null,
    val asyncWork: AsyncWork? = null
) : WorkResult()

internal sealed class AddTrackableWorkResult : WorkResult() {
    internal data class AlreadyIn(
        val trackableStateFlow: MutableStateFlow<TrackableState>,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class Success(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : AddTrackableWorkResult()

    internal data class Fail(
        val trackable: Trackable,
        val exception: Throwable?,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class WorkDelayed(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : AddTrackableWorkResult()
}

internal sealed class ConnectionCreatedWorkResult : WorkResult() {
    internal data class RemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>
    ) : ConnectionCreatedWorkResult()

    internal data class PresenceSuccess(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: (presenceMessage: PresenceMessage) -> Unit,
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : ConnectionCreatedWorkResult()

    internal data class PresenceFail(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val presenceUpdateListener: (presenceMessage: PresenceMessage) -> Unit,
        val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    ) : ConnectionCreatedWorkResult()
}

internal sealed class ConnectionReadyWorkResult : WorkResult() {
    internal data class RemovalRequested(
        val trackable: Trackable,
        val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
        val result: Result<Unit>
    ) : ConnectionReadyWorkResult()

    internal object OptimalConnectionReady : ConnectionReadyWorkResult()

    internal data class NonOptimalConnectionReady(
        val trackable: Trackable,
        val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    ) : ConnectionReadyWorkResult()
}

internal sealed class RetrySubscribeToPresenceWorkResult : WorkResult() {
    internal object TrackableRemoved : RetrySubscribeToPresenceWorkResult()

    internal object ChannelFailed : RetrySubscribeToPresenceWorkResult()

    internal data class Success(
        val trackable: Trackable,
    ) : RetrySubscribeToPresenceWorkResult()

    internal data class Failure(
        val trackable: Trackable,
        val presenceUpdateListener: (presenceMessage: PresenceMessage) -> Unit,
    ) : RetrySubscribeToPresenceWorkResult()
}

internal sealed class RemoveTrackableWorkResult : WorkResult() {
    internal data class Success(
        val callbackFunction: ResultCallbackFunction<Boolean>,
        val trackable: Trackable,
    ) : RemoveTrackableWorkResult()

    internal data class Fail(
        val callbackFunction: ResultCallbackFunction<Boolean>,
        val exception: Throwable,
    ) : RemoveTrackableWorkResult()

    internal data class NotPresent(
        val callbackFunction: ResultCallbackFunction<Boolean>
    ) : RemoveTrackableWorkResult()
}

internal sealed class TrackableRemovalRequestedWorkResult : WorkResult() {
    internal object StopConnectionCompleted : TrackableRemovalRequestedWorkResult()
}

internal sealed class AddTrackableFailedWorkResult : WorkResult() {
    internal object StopConnectionCompleted : AddTrackableFailedWorkResult()
}

internal sealed class DisconnectSuccessWorkResult : WorkResult() {
    internal object StopConnectionCompleted : DisconnectSuccessWorkResult()
}
