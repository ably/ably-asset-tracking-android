package com.ably.tracking.common

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
