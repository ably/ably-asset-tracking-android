package com.ably.tracking.common

import com.ably.tracking.FailureResult
import com.ably.tracking.Result
import com.ably.tracking.SuccessResult

/**
 * Utility to convert a Kotlin Result as reported to a Kotlin handler,
 * to a Java Result to be reported to a Java listener.
 */
fun Result<Unit>.toJava(): Result<Void?> = when (this) {
    is SuccessResult<Unit> -> SuccessResult(null)
    is FailureResult<Unit> -> FailureResult(this.errorInformation)
}
