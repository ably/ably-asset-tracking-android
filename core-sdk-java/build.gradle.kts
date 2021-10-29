plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint")
}
apply{from("../jacoco.gradle")}

dependencies {
    api(project(":core-sdk"))
}

apply{from("../publish.gradle")}
