plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
}

android {
    defaultConfig {
        consumerProguardFiles( "consumer-rules.pro")
    }
}
apply { from("../jacoco.gradle") }
apply { from("../publish.gradle") }
