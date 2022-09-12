package com.ably.tracking.annotations

@RequiresOptIn(message = "This is an experimental API. It may change in the future.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Experimental // Opt-in requirement annotation
