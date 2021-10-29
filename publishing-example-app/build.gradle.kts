val ablyCoreVersion:String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("org.jlleitschuh.gradle.ktlint")
}

val useCrashlytics = project.file("google-services.json").exists()
if (useCrashlytics) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
android {
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.ably.tracking.example.publisher"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}

dependencies {
    implementation(project(":publishing-sdk"))
    implementation("io.ably:ably-android:$ablyCoreVersion")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("com.amplifyframework:aws-storage-s3:1.4.1")
    implementation("com.amplifyframework:aws-auth-cognito:1.4.1")
    implementation("pub.devrel:easypermissions:3.0.0")
    implementation("com.mapbox.mapboxsdk:mapbox-android-sdk:8.6.6")

    if (useCrashlytics) {
        // The BoM for the Firebase platform (it specifies the versions for Firebase dependencies).
        implementation(platform("com.google.firebase:firebase-bom:26.8.0"))

        // Firebase dependencies, which should not specifies versions as those are provided
        // for us because we"re including the BoM above.
        // see: https://firebase.google.com/docs/crashlytics/get-started?platform=android
        implementation("com.google.firebase:firebase-crashlytics")
    }
}
