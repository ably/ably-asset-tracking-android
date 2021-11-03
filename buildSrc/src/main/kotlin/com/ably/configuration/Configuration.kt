package com.ably.configuration

object Configuration {
    const val compileSdk = 30
    const val targetSdk = 30
    const val minSdkLower = 21
    const val minSdkHigher = 24
    const val majorVersion = 1
    const val minorVersion = 2
    const val patchVersion = 8
    const val versionName = "$majorVersion.$minorVersion.$patchVersion"
    const val snapshotVersionName = "$majorVersion.$minorVersion.${patchVersion + 1}-SNAPSHOT"
    const val artifactGroup = "io.ably"
}