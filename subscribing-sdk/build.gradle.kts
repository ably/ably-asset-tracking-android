plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
}
apply{
    from("../jacoco.gradle")
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":core-sdk"))
    implementation(project(":common"))
}

apply{
    from("../publish.gradle")
}
