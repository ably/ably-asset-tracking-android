package com.ably.tracking.common.logging

/**
 * Creates logging tag based on the name of the [loggingObject]'s class.
 */
fun createLoggingTag(loggingObject: Any): String = "[${loggingObject::class.simpleName}]"
