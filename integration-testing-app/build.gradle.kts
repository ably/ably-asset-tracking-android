val ablyCoreVersion:String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    defaultConfig {
        applicationId = "com.ably.tracking.tests"
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":subscribing-sdk"))
    implementation(project(":publishing-sdk"))
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.ably:ably-android:$ablyCoreVersion")
    implementation("io.jsonwebtoken:jjwt:0.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
}
