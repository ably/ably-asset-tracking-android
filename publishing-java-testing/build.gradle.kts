plugins {
    id("com.android.application")
}

android {
    defaultConfig {
        applicationId = "com.ably.tracking.example.javapublisher"
    }
}

dependencies {
    implementation(project(":publishing-sdk-java"))
}
