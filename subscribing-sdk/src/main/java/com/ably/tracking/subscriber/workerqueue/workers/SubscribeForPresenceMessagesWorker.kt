package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeForPresenceMessagesWorker(
    private val ably: Ably,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): Properties {
        doAsyncWork {
            val currentPresenceMessagesResult = ably.getCurrentPresence(trackableId)
            val initialPresenceMessages: List<PresenceMessage> = try {
                currentPresenceMessagesResult.getOrThrow()
            } catch (error: Throwable) {
                postDisconnectWork(postWork, Result.failure(error))
                return@doAsyncWork
            }

            val subscribeForPresenceMessagesResult = ably.subscribeForPresenceMessages(
                trackableId = trackableId,
                emitCurrentMessages = false,
                listener = { postWork(WorkerSpecification.UpdatePublisherPresence(it)) }
            )
            if (subscribeForPresenceMessagesResult.isFailure) {
                postDisconnectWork(postWork, subscribeForPresenceMessagesResult)
                return@doAsyncWork
            }

            postWork(WorkerSpecification.ProcessInitialPresenceMessages(initialPresenceMessages, callbackFunction))
        }
        return properties
    }

    private fun postDisconnectWork(postWork: (WorkerSpecification) -> Unit, result: Result<Unit>) {
        postWork(
            WorkerSpecification.Disconnect(trackableId) {
                callbackFunction(result)
            }
        )
    }
}
