// This block was added for Dokka, per the instructions here:
// https://github.com/Kotlin/dokka#using-the-gradle-plugin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include(":android-test-common",
        ":test-common",
        ":common",
        ":core-sdk",
        ":core-sdk-java",
        ":integration-testing-app",
        ":publishing-sdk",
        ":publishing-sdk-java",
        ":publishing-example-app",
        ":publishing-java-testing",
        ":subscribing-sdk",
        ":subscribing-sdk-java",
        ":subscribing-example-app",
        ":subscribing-java-testing")
