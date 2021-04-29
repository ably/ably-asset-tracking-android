package com.ably.tracking.annotations

/**
 * An immutable data structure (a.k.a. DTO) which is received by or transmitted from the local library
 * from/to an external system, most likely another component of the wider Ably Asset Tracking ecosystem.
 * This includes data structures which are sent over the network as well as data structures which are
 * persisted to files imported or exported by this library.
 */
@Target(AnnotationTarget.CLASS)
annotation class Shared
