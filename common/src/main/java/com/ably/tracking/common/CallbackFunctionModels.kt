package com.ably.tracking.common

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle events using that function as a callback (typically using a lamda expression).
 */
typealias CallbackFunction<T> = (T) -> Unit

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle an event indicating the result of an asynchronous operation.
 */
typealias ResultCallbackFunction<T> = CallbackFunction<Result<T>>

/**
 * Utility function adapts [Continuation] to fit in [ResultCallbackFunction] signature. Callback result is unpacked and
 * used to resume the continuation.
 */
fun <T> Continuation<T>.wrapInResultCallback(): ResultCallbackFunction<T> =
    { result ->
        try {
            resume(result.getOrThrow())
        } catch (exception: Exception) {
            resumeWithException(exception)
        }
    }
