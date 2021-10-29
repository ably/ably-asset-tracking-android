val ablyCoreVersion: String by project

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(project(":core-sdk"))
    implementation("io.ably:ably-android:$ablyCoreVersion")
}
