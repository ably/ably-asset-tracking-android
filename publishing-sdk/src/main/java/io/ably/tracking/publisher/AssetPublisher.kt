package io.ably.tracking.publisher

interface AssetPublisher {
    companion object Factory {
        /**
         * Returns the default builder of Publisher instances.
         */
        fun publishers(): PublisherBuilder {
            // TODO ensure this can be called from Java - may need @JvmStatic annotation
            // https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects
            TODO("To be implemented.")
        }
    }

    interface PublisherBuilder
}
