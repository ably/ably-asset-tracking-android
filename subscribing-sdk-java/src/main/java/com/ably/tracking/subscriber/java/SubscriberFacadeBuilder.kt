package com.ably.tracking.subscriber.java

import com.ably.tracking.subscriber.Subscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class SubscriberFacadeBuilder(
    private val builder: Subscriber.Builder
) : SubscriberFacade.Builder, Subscriber.Builder by builder {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun startAsync(): CompletableFuture<SubscriberFacade> {
        return scope.future { DefaultSubscriberFacade(builder.start()) }
    }
}
