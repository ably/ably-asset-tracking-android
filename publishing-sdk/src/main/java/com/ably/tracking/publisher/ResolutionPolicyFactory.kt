package com.ably.tracking.publisher

/**
 * Defines the methods to be implemented by resolution policy factories, whose responsibility it is to create
 * a new [ResolutionPolicy] instance when a [Publisher] is [started][Publisher.Builder.start].
 */
interface ResolutionPolicyFactory {
    /**
     * This method will be called once for each [Publisher] instance started by any [Builder][Publisher.Builder]
     * instance against which this [Factory] was [registered][Publisher.Builder.resolutionPolicy].
     *
     * Calling methods on [hooks] after this method has returned will throw an exception.
     *
     * Calling methods on [methods] after this method has returned is allowed and expected.
     *
     * @param hooks Methods which may be called while inside this method implementation, but not after.
     * @param methods Methods which may be called after this method has returned.
     * @return A resolution policy to be used for the lifespan of the associated [Publisher].
     */
    fun createResolutionPolicy(hooks: ResolutionPolicyHooks, methods: ResolutionPolicyMethods): ResolutionPolicy
}
