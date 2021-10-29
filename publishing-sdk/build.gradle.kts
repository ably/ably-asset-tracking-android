val ablyCoreVersion:String by project

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.kapt")
}
apply{from("../jacoco.gradle")}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":core-sdk"))
    implementation(project(":common"))
    implementation("io.ably:ably-android:$ablyCoreVersion")

    // The MapBox Navigation SDK for Android.
    // We"re not using the pre-built UI components, so just need the core dependency.
    // https://docs.mapbox.com/android/navigation/overview/
    implementation("com.mapbox.navigation:core:1.6.1") {
        // We provide a custom trip notification so we exclude the default one.
        // https://docs.mapbox.com/android/navigation/guides/modularization/#tripnotification
        exclude(group = "com.mapbox.navigation",module = "notification")
    }

    // Dependencies needed for replacing Mapbox modules.
    // https://docs.mapbox.com/android/navigation/guides/modularization/#replacing-a-module
    compileOnly("com.mapbox.base:annotations:0.4.0")
    kapt("com.mapbox.base:annotations-processor:0.4.0")

    // Only the Core SDK portion of the MapBox Maps SDK for Android.
    // https://docs.mapbox.com/android/maps/overview/
    // https://docs.mapbox.com/android/core/overview/
    implementation("com.mapbox.mapboxsdk:mapbox-android-core:3.1.1")

    // Advanced geospatial analysis - used i.e. for calculating distance between points.
    // https://docs.mapbox.com/android/java/guides/turf/
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:5.7.0")

    // Required for FusedLocationProviderClient that"s used in Google location engine implementation
    // https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient
    implementation("com.google.android.gms:play-services-location:17.1.0")
}
apply { from("../publish.gradle") }
