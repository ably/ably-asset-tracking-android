package com.ably.tracking.common

import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/**
 * Creates a single thread dispatcher.
 *
 * To assure that we process our events in the FIFO order we need to use a single threaded dispatcher.
 * Because of that, all calls to the [launch] will execute in the order of invocation.
 *
 * Because threads are expensive resources we don't want to create a separate one for each instance.
 * Therefore, we have one dispatcher shared across all SDK instances. In an edge case scenario where
 * multiple instances are active at the same time, this could theoretically lead to some slowdowns,
 * as all the work performed by all the instance will be handled on the same thread.
 *
 * Because the dispatcher is shared it is never explicitly stopped by the SDK but it will be implicitly
 * stopped by the OS when the app is killed.
 */
fun createSingleThreadDispatcher() = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
