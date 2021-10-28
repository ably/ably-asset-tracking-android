val ablyCoreVersion:String by project

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(project(":core-sdk"))
    implementation("io.ably:ably-android:$ablyCoreVersion")
}

android {
    compileOptions {
        kotlinOptions.freeCompilerArgs += listOf(
            "-module-name", "com.ably.tracking.common"
        )
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

apply { from("../publish.gradle") }

