package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.ConnectionException
import com.ably.tracking.TrackableState
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultHandler
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias AsyncWork<T> = (suspend () -> T)

/**
 * This sealed class represents a base result from concrete  [Worker] instances after they finish doing their work.
 * It's also intended to be returned by [AsyncWork] coroutines.
 * **/
internal sealed class WorkResult

/**
 * A special [WorkResult] that contains an optional [syncWorkResult] and [asyncWork]
 *
 * @param syncWorkResult : Any work result that has resulted from synchronous work. This work result is expected to be
 * returned and processed in a blocking fashion
 * @param asyncWork is any work that is not immediately executed but returned to caller for background execution.
 *
 * [asyncWork] is a suspending work and it is intended to be launched in a different coroutine. However there is
 * nothing that prevents callers from executing this in the same coroutine. If any use case like this arise, you
 * should document the reason of such usage.
 * **/
internal data class SyncAsyncResult(
    val syncWorkResult: WorkResult? = null,
    val asyncWork: AsyncWork<WorkResult>? = null
) : WorkResult()

internal sealed class AddTrackableWorkResult : WorkResult() {
    internal data class AlreadyIn(
        val trackableStateFlow: MutableStateFlow<TrackableState>,
        val handler: ResultHandler<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class Success(
        val trackable: Trackable,
        val handler: ResultHandler<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()

    internal data class Fail(
        val trackable: Trackable,
        val exception: Throwable?,
        val handler: ResultHandler<StateFlow<TrackableState>>
    ) : AddTrackableWorkResult()
}

internal sealed class ConnectionCreatedWorkResult : WorkResult() {
    internal data class RemovalRequested(
        val trackable: Trackable,
        val handler: ResultHandler<StateFlow<TrackableState>>,
        val successfulDisconnect: Boolean,
        val exception: ConnectionException? = null
    ) : ConnectionCreatedWorkResult()

    internal data class PresenceSuccess(
        val trackable: Trackable,
        val handler: ResultHandler<StateFlow<TrackableState>>,
        val presenceUpdateListener: (trackable: Trackable, presenceMessage: PresenceMessage) -> Unit
    ) : ConnectionCreatedWorkResult()

    internal data class PresenceFail(
        val trackable: Trackable,
        val handler: ResultHandler<StateFlow<TrackableState>>,
        val exception: ConnectionException
    ) : ConnectionCreatedWorkResult()
}
