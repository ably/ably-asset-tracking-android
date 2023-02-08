# Contributing to the Ably Asset Tracking SDKs for Android

## Java version

To run the `./gradlew` script locally, you need to be using a version of Java which is supported by the version of Gradle that this project uses.

The version of Gradle that this project uses is specified by the `distributionUrl` in [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties). To find out which versions of Java are compatible with this version of Gradle, consult the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#java).

If you are using [`asdf`](https://asdf-vm.com) to manage tool versions, then you can rely on the `.tool-versions` file in this repository, which is configured to use an appropriate version of Java.

## Running the test proxy server

In order to run the `NetworkConnectivityTests`, you will need to be running an instance of the [SDK Test Proxy](https://github.com/ably/sdk-test-proxy) on your development machine. You can do this by executing `./gradlew run` inside the `external/sdk-test-proxy` directory.

## Development

This repository is structured as a Gradle [Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html).
To run checks, tests and assemble all SDK and app projects from the command line use:

- macOS: `./gradlew check assemble`
- Windows: `gradle.bat check assemble`

These are the same Gradle tasks that we [run in CI](.github/workflows).

Before contributing to this repository please read the [architectural notes](ArchitecturalNotes.md).

### Recommended IDE

We developed and continue to maintain this SDK using [Android Studio](https://developer.android.com/studio), so this is the IDE (Integrated Development Environment) we recommend you use.
From the dialog presented by `File` > `Open...` / `Open an Existing Project`, select the repository root folder and Studio's built-in support for Gradle projects will do the rest.

Android Studio is the official IDE for Android app development and is based on top of [IntelliJ IDEA](https://www.jetbrains.com/idea/).
This means that, more often than not, sources of help you will find in relation to IDE questions or problems you face will be from JetBrains and related to IntelliJ directly. Most of the time these sources of help are also relevant to Android Studio, but your mileage may vary.

Because of IntelliJ's [approach to Project Security](https://www.jetbrains.com/help/idea/project-security.html) you will need to explicitly trust the project when you open it. Previewing in safe mode will not give you the full IDE experience.

### MapBox SDK dependency

After cloning this repository for the first time, you will likely find that opening it in Android Studio or attempting to use Gradle from the command line (e.g. `./gradlew tasks`) will produce the following **FAILURE**:

    Could not get unknown property 'MAPBOX_DOWNLOADS_TOKEN' for project ':publishing-sdk' of type org.gradle.api.Project.

This is normal, and is easy to fix.

MapBox's Maps SDK for Android documentation [suggests](https://docs.mapbox.com/android/maps/overview/#configure-credentials) configuring your secret token in `~/.gradle/gradle.properties`, which makes sense as it keeps it well away from the repository itself to avoid accidental checkin.

There are, of course, [many other ways](https://docs.gradle.org/current/userguide/build_environment.html) to inject project properties into Gradle builds - all of which should work for this `MAPBOX_DOWNLOADS_TOKEN` property.

### API Keys and Access Tokens

The following secrets need configuring in a similar manner to that described above for the MapBox SDK Dependency `MAPBOX_DOWNLOADS_TOKEN`:

- `ABLY_API_KEY`
- `MAPBOX_ACCESS_TOKEN`
- `GOOGLE_MAPS_API_KEY`

### Runtime Secrets and Connected Checks

The Gradle build scripts react to values assigned to the `runtimeSecrets` property.
This is a property unique to the projects in this repository, altering the build configuration depending on the downstream needs of the build, in respect of `BuildConfig` availability and values of `ABLY_API_KEY` and `MAPBOX_ACCESS_TOKEN` (both supplied via Gradle properties).

| `runtimeSecrets` value | Build Configuration | Notes |
| ---------------------- | ------------------- | ----- |
| `FOR_ALL_PROJECTS_BECAUSE_WE_ARE_RUNNING_INTEGRATION_TESTS` | Production secrets injected into all projects, for both `release` and `debug` build types. | Allows integration tests (connected checks, the `androidTest` source set in each project) to have access to these secrets. Used by the [emulate](.github/workflows/emulate.yml) workflow. |
| `USE_DUMMY_EMPTY_STRING_VALUES` | Dummy secrets injected only into app projects. This allows the projects to build without production secrets needing to be supplied via Gradle properties. | This means that any app or live-service integration test builds that attempt to use these secret values at runtime will fail. Used by the [check](.github/workflows/check.yml), [docs](.github/workflows/docs.yml) and publishing workflows. |
| _either undefined or any other value_ | Production secrets injected only into app projects. | Ensures that they are not accidentally exposed to any of the SDK projects. Used, implicitly, by the [assemble](.github/workflows/assemble.yml) workflow. |

It is a little bit hacky and there might be another way to do this in a more Gradle or Android idiomatic manner, however it suits the needs of our project build for the time being and does not change or otherwise alter the SDK products we publish.

For local development purposes, most developers will find that the most helpful general purpose configuration is to put the following line into their `~/.gradle/gradle.properties` file:

    runtimeSecrets: FOR_ALL_PROJECTS_BECAUSE_WE_ARE_RUNNING_INTEGRATION_TESTS

### Debugging Gradle Task Dependencies

There isn't an out-of-the-box command provided by Gradle to provide a readable breakdown of which tasks in the build are configured to rely upon which other tasks. The `--dry-run` switch helps a bit, but it provides a flat view which doesn't provide the full picture.

We could have taken the option to include some Groovy code or a plugin in the root project configuration to provide a full task tree view, however it's strictly not needed to be part of the sources within this repository to build the projects as it's only a tool to help with debugging Gradle's configuration.

If such a view is required then we suggest installing [this Gradle-global, user-level init script](https://github.com/dorongold/gradle-task-tree#init-script-snippet), within `~/.gradle/init.gradle` as [described in the Gradle documentation](https://docs.gradle.org/current/userguide/init_scripts.html#sec:using_an_init_script). Once the init script is in place then, for example, the Gradle `check` task can be examined using:

    ./gradlew check taskTree --no-repeat

The `taskTree` task requires a preceding task name and can be run per project, a fact that's visible with:

    ./gradlew tasks --all | grep taskTree

### Automated Testing

There are a few things that you need to be aware of when writing automated tests.

#### Mapbox Replays

When writing automated tests using Mapbox's replay engine, you may notice that the first location event appears to be replayed twice when a trip is started.

This is expected, and is due to the fact that our event listeners get registered within Mapbox for both "location updates" and also a "first location" -
which tells us where the asset is at the start of the trip. Subsequent location updates will only be received once.

## Secrets Required to Release

This section defines the names of the
[GitHub secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
defined to be used by our publishing workflows.

### Code Signing

[OpenGPG Signatory Credentials](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials)
to be used by Gradle's
[Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html):

- `MAVEN_SIGNING_KEY_ID`: The public key ID.
- `MAVEN_SIGNING_KEY_PASSWORD`: The passphrase that was used to protect the private key.
- `MAVEN_SIGNING_KEY_RING_FILE_BASE64`: The contents of the secret key ring file that contains the private key, base64 encoded so that it can be injected as a GitHub secret (encode from macOS using `openssl base64 < signing.key.gpg | pbcopy`).

### Sonatype for Maven Central

Details for the Sonatype identity to be used to publish to our Ably's `com.ably` project ([OSSRH-68447](https://issues.sonatype.org/browse/OSSRH-68447)):

- `OSSRH_USERNAME`: The Sonatype user.
- `OSSRH_PASSWORD`: The password for the Sonatype user.

## Secrets Required to Distribute the Example Apps

The primary (Kotlin, not Java) example apps
([Publishing](publishing-example-app/)
and
[Subscribing](subscribing-example-app/))
were initially developed with a focus on them being useful to developers working on this codebase to provide them with a test harness for quick, manual, local/desk-bound testing as they worked on features and bug fixes.
These example apps, however, also grew to attain functionality that helps us and the wider Ably team do 'in the field' manual testing more akin to QA (Quality Assurance) testing.

In order to allow these example apps to be installed OTA (Over-The-Air) onto the Android devices of manual/QA testers where there is no development environment (e.g. Android Studio) attached we have configured their Gradle projects to support [Firebase App Distribution](https://firebase.google.com/docs/app-distribution).

How these secrets are used by the Gradle build runtime can be best observed by inspecting
[the `publish-example-apps` workflow file](.github/workflows/publish-example-apps.yml).

**Partially Supported:**
We have only configured this for the Publishing Example project so far.
We will add it to the Subscribing Example App project when we work on
https://github.com/ably/ably-asset-tracking-android/issues/957.

### Android App Signing Key

The secrets used to configure the Android app signing configuration are called:

| Secret Name | Gradle Build Input Mechanism | Description |
| ----------- | ---------------------------- | ----------- |
| `AAT_SDK_EXAMPLE_APPS_ANDROID_SIGNING_JKS_FILE_BASE64` | Hydrated (decoded from base64) to file at `publishing-example-app/signing.jks` | This is a Java KeyStore file and can be created using Android Studio using the instructions [here](https://developer.android.com/studio/publish/app-signing#generate-key). Configures [the `storeFile` property](https://developer.android.com/reference/tools/gradle-api/4.2/com/android/build/api/dsl/SigningConfig#storeFile:java.io.File). |
| `AAT_SDK_EXAMPLE_APPS_ANDROID_SIGNING_KEY_ALIAS` | Property with the same name. | Configures [the `keyAlias` property](https://developer.android.com/reference/tools/gradle-api/4.2/com/android/build/api/dsl/SigningConfig#keyAlias:kotlin.String). |
| `AAT_SDK_EXAMPLE_APPS_ANDROID_SIGNING_KEY_PASSWORD` | Property with the same name. | Configures [the `keyPassword` property](https://developer.android.com/reference/tools/gradle-api/4.2/com/android/build/api/dsl/SigningConfig#keyPassword:kotlin.String). |
| `AAT_SDK_EXAMPLE_APPS_ANDROID_SIGNING_STORE_PASSWORD` | Property with the same name. | Configures [the `storePassword` property](https://developer.android.com/reference/tools/gradle-api/4.2/com/android/build/api/dsl/SigningConfig#storePassword:kotlin.String). |

### Google Services

This file is required by the
[Google Services Gradle Plugin](https://developers.google.com/android/guides/google-services-plugin)
to (optionally) supply the Gradle build runtime with the information it requires to:

- Build [Firebase Crashlytics](https://firebase.google.com/products/crashlytics) support into the example app runtime
- Build [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) support into the Gradle build runtime

The secret, for the Publishing Example App, is called `PUBLISHING_EXAMPLE_APP_GOOGLE_SERVICES_JSON`.

### Google Service Account

A
[Service Account](https://cloud.google.com/iam/docs/service-accounts)
is required to upload example apps to the Firebase App Distribution service and contains the secret information required by the Gradle build runtime to authorise with that service using
[the `appDistributionUploadRelease` task](https://firebase.google.com/docs/app-distribution/android/distribute-gradle#step_4_distribute_your_app_to_testers)
that is provided by
[the Firebase App Distribution Gradle plugin](https://maven.google.com/web/index.html?q=firebase-appdistribution-gradle).

The secret is called `ASSET_TRACKING_SDKS_GSERVICEACCOUNT_PRIVATE_KEY_JSON`.

Creating the Service Account and then providing it the necessary roles and permissions can be done in a number of ways but for our initial setup and testing of this capability was achieved using Google Cloud's
[IAM and admin Console (Web UI)](https://console.cloud.google.com/iam-admin/).
The
[Policy troubleshooter tool](https://cloud.google.com/policy-intelligence/docs/troubleshoot-access)
therein is highly recommended as it was helpful to work out which roles needed to be given to this account in order for it to succeed in authorising with Firebase App Distribution.

For the `firebaseappdistro.releases.update` permission for the Firebase project resource we found that the following roles were required for this service account to allow the Gradle task to succeed in uploading to Firebase App Distribution:

- Editor
- Firebase App Distribution Admin
- Owner

**Caveat**: The initial author of this guidance is not an expert in Firebase or IAM. Your mileage may vary. Stuff changes. It is strongly suggested to read Google Docs yourself for canonical guidance, or consult an expert.

**OpenID Connect (OIDC):** We should be using this, but we're not yet, but will once we work on
https://github.com/ably/ably-asset-tracking-android/issues/958.

## Release Process

Releases should always be made through a release pull request (PR), which needs to bump the version number and add to the [change log](CHANGELOG.md).
For an example of a previous release PR, see [#298](https://github.com/ably/ably-asset-tracking-android/pull/298).

The release process must include the following steps:

1. Ensure that all work intended for this release has landed to `main`
2. Create a release branch named like `release/1.2.3`
3. Add a commit to bump the version number - this commit should also include an update to the root `README.md`,where
the latest version number is explicitly referenced. You must also bump version code by 1 for each version bump.
4. Add a commit to update the change log
5. Push the release branch to GitHub
6. Open a PR for the release against the release branch you just pushed
7. Gain approval(s) for the release PR from maintainer(s)
8. Land the release PR to `main`
9. Create a tag named like `v1.2.3` and push it to GitHub
10. Run the publish workflows:
    - These are manually triggered, where you supply the version number so the script publishes only up to that tag
    - They must be run from the `main` branch
    - First workflow: [Maven Central](https://github.com/ably/ably-asset-tracking-android/actions/workflows/publish-maven-central.yml)
    - Second workflow: [GitHub Packages](https://github.com/ably/ably-asset-tracking-android/actions/workflows/publish-github-packages.yml)
11. Create the release on GitHub including populating the release notes
12. Create the entry on the [Ably Changelog](https://changelog.ably.com/) (via [headwayapp](https://headwayapp.co/))

We tend to use [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator) to collate the information required for a change log update.
Your mileage may vary, but it seems the most reliable method to invoke the generator is something like:
`github_changelog_generator -u ably -p ably-asset-tracking-android --since-tag v1.0.0 --output delta.md`
and then manually merge the delta contents in to the main change log (where `v1.0.0` in this case is the tag for the previous release).

### Checking that the release reached Maven Central

There is always a slight delay after our publish workflow has completed until the release appears at Maven Central, measuring in minutes.

The following sites will get updated once the release is available:

- [central.sonatype.com](https://central.sonatype.com/search?sort=name&namespace=com.ably.tracking) - their newest Web UI (at the time of writing this, at least) so is likely to be the quickest to get updated
- [search.maven.org](https://search.maven.org/search?q=g:com.ably.tracking)
- [mvnrepository.com](https://mvnrepository.com/artifact/com.ably.tracking)
