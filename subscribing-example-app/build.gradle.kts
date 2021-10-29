plugins {
   id("com.android.application")
   id("kotlin-android")
   id("kotlin-android-extensions")
   id("org.jlleitschuh.gradle.ktlint")
}

android {
    defaultConfig {
        applicationId = "com.ably.tracking.example.subscriber"
        resValue(type = "string",name = "google_maps_api_key",value ="${property("GOOGLE_MAPS_API_KEY")}")
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}

dependencies {
    implementation(project(":subscribing-sdk"))
    implementation("com.google.android.gms:play-services-maps:17.0.0")
}
