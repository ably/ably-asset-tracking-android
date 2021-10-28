val ably_core_version:String by project

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(project(":core-sdk"))
    implementation("io.ably:ably-android:$ably_core_version")
}
