package com.ably.tracking.annotations

/**
 * An immutable data structure (a.k.a. DTO) which is received by or transmitted from the local library
 * from/to an external system, most likely another component of the wider Ably Asset Tracking ecosystem.
 * This includes data structures which are sent over the network as well as data structures which are
 * persisted to files imported or exported by this library.
 *
 * Fields on data structures labelled with this annotation must use GSON's SerializedName annotation
 * to explicitly set the field name to be used in JSON representations. This requirement has come from
 * an issue we have seen with apps built using this SDK, whereby proguard obfuscation (minify) causes
 * a change in runtime serialization behaviour. See this issue for more details:
 * https://github.com/ably/ably-asset-tracking-android/issues/396
 */
@Target(AnnotationTarget.CLASS)
annotation class Shared
