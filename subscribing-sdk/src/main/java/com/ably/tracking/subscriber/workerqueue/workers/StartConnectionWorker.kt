package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.doOnFatalAblyFailure
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.emitStateEventsIfRequired()
        callbackFunction(Result.success(Unit))
        doAsyncWork { connect(postWork) }
        return properties
    }

    private suspend fun connect(
        postWork: (WorkerSpecification) -> Unit
    ) {
        ably.startConnection().doOnFatalAblyFailure { errorInformation ->
            val connectionStateChange =
                ConnectionStateChange(ConnectionState.FAILED, errorInformation)
            postWork(WorkerSpecification.UpdateConnectionState(connectionStateChange))
            return@connect
        }

        ably.connect(trackableId, useRewind = true, willSubscribe = true)
            .doOnFatalAblyFailure { errorInformation ->
                val connectionStateChange =
                    ConnectionStateChange(ConnectionState.FAILED, errorInformation)
                postWork(WorkerSpecification.UpdateConnectionState(connectionStateChange))
                return@connect
            }

        postWork(WorkerSpecification.SubscribeForPresenceMessages)
        postWork(WorkerSpecification.EnterPresence(trackableId))
    }
}
