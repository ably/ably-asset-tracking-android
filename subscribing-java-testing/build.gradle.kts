plugins {
    id("com.android.application")
}

android {
    defaultConfig {
        applicationId = "com.ably.tracking.example.javasubscriber"
    }
}

dependencies {
    implementation(project(":subscribing-sdk-java"))
}
