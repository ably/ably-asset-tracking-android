# Ably Asset Tracking SDKs for Android

![.github/workflows/check.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/check.yml/badge.svg)
![.github/workflows/emulate.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/emulate.yml/badge.svg)
![.github/workflows/assemble.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/assemble.yml/badge.svg)

## Overview

Ably Asset Tracking SDKs provide an easy way to track multiple assets with realtime location updates powered by [Ably](https://ably.io/) realtime network and Mapbox [Navigation SDK](https://docs.mapbox.com/android/navigation/overview/) with location enhancement.

**Status:** this is a preview version of the SDKs. That means that it contains a subset of the final SDK functionality, and the APIs are subject to change. The latest release of the SDKs is available in the [Releases section](https://github.com/ably/ably-asset-tracking-android/releases/tag/1.0.0-preview.1) of this repo

Ably Asset Tracking is:

- **easy to integrate** - comprising two complementary SDKs with easy to use APIs, available for multiple platforms:
    - Asset Publishing SDK, for embedding in apps running on the courier's device
    - Asset Subscribing SDK, for embedding in apps runnong on the customer's observing device
- **extensible** - as Ably is used as the underlying transport, you have direct access to your data and can use Ably integrations for a wide range of applications in addition to direct realtime subscriptions - examples include:
    - passing to a 3rd party system
    - persistence for later retrieval
- **built for purpose** - the APIs and underlying functionality are designed specifically to meet the requirements of a range of common asset tracking use-cases

In this repository there are two SDKs for Android devices:

- the [Asset Publishing SDK](publishing-sdk/)
- the [Asset Subscribing SDK](subscribing-sdk/)

The Asset Publishing SDK is used to get the location of the assets that need to be tracked.

Here is an example of how the Asset Publishing SDK can be used:

```kotlin
// Prepare Resolution Constraints for the Resolution Policy
val exampleConstraints = DefaultResolutionConstraints(
    DefaultResolutionSet( // this constructor provides one Resolution for all states
        Resolution(
            accuracy = Accuracy.BALANCED,
            desiredInterval = 1000L,
            minimumDisplacement = 1.0
        )
    ),
    proximityThreshold = DefaultProximity(spatial = 1.0),
    batteryLevelThreshold = 10.0f,
    lowBatteryMultiplier = 2.0f
)

// Initialise and Start the Publisher
val publisher = Publisher.publishers() // get the Publisher builder in default state
    .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
    .map(MapConfiguration(MAPBOX_ACCESS_TOKEN)) // provide Mapbox configuration with credentials
    .androidContext(this) // provide Android runtime context
    .profile(RoutingProfile.CYCLING) // provide mode of transportation for better location enhancements
    .start()

// Start tracking an asset
publisher.track(
    Trackable(
        trackingId, // provide a tracking identifier for the asset
        constraints = exampleConstraints // provide a set of Resolution Constraints
    ),
    {
        when (it) {
            is SuccessResult -> {
                // TODO handle asset tracking started successfully
            }
            is FailureResult -> {
                // TODO handle asset tracking could not be started
            }
        }
    }
)
```

Asset Subscribing SDK is used to receive the location of the required assets.

Here is an example of how Asset Subscribing SDK can be used:

```kotlin
// Initialise and Start the Subscriber
val subscriber = Subscriber.subscribers() // Get an AssetSubscriber
    .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
    .enhancedLocations { } // provide a function to be called when enhanced location updates are received
    .resolution( // request a specific resolution to be considered by the publisher
        Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
    )
    .trackingId(trackingId) // provide the tracking identifier for the asset that needs to be tracked
    .assetStatus { } // provide a function to be called when the asset changes online/offline status
    .start() // start listening for updates

// Request a different resolution when needed.
subscriber.sendChangeRequest(
    Resolution(Accuracy.MAXIMUM, desiredInterval = 100L, minimumDisplacement = 2.0),
    {
        when (it) {
            is SuccessResult -> {
                // TODO change request submitted successfully
            }
            is FailureResult -> {
                // TODO change request could not be submitted
            }
        }
    }
)
```

## Example Apps

This repository also contains example apps that showcase how the Ably Asset Tracking SDKs can be used:

- the [Asset Publishing example app](publishing-example-app/)
- the [Asset Subscribing example app](subscribing-example-app/)

To build these apps you will need to specify [credentials](#api-keys-and-access-tokens) in Gradle properties.

## Development

This repository is structured as a Gradle [Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html).

To run checks, tests and assemble all SDK and app projects from the command line use:

- macOS: `./gradlew check assemble`
- Windows: `gradle.bat check assemble`

These are the same Gradle tasks that we [run in CI](.github/workflows).

The recommended IDE for working on this project is [Android Studio](https://developer.android.com/studio).
From the dialog presented by `File` > `Open...` / `Open an Existing Project`, select the repository root folder and Studio's built-in support for Gradle projects will do the rest.

### Android Runtime Requirements

These SDKs require a minimum of Android API Level 21 at runtime.

### Coding Conventions and Style Guide

- Use best, current practice wherever possible.
- Kotlin is our primary development language for this project (in respect of SDK interfaces and implementation, as well as example app development):
    - We must keep in mind that some developers may choose to utilise the SDKs we build from a Java codebase (see [Calling Kotlin from Java](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html))
    - We should do our best to avoid "writing Kotlin with a Java accent":
        - published [Kotlin idioms](https://kotlinlang.org/docs/reference/idioms.html) should be utilised
        - strict linting and static analysis rules should be applied to all code, including unit and integration tests - Kotlin's Coding Conventions may be a starting point but all rules **must** fail the build when built from the command line (i.e. `./gradlew`, especially including CI / CD runs)

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

### Resolution Policies

In order to provide application developers with flexibility when it comes to choosing their own balance between higher frequency of updates and optimal battery usage, we provide several ways for them to define the logic used to determine the frequency of updates:

- by implementing a custom `ResolutionPolicy` - providing the greatest flexibility
- by using the default `ResolutionPolicy` implementation - with the controls provided by `DefaultResolutionPolicyFactory` and `DefaultResolutionConstraints`

#### Using the Default Resolution Policy

The simplest way to control the frequency of updates is by providing parameters in the form of `DefaultResolutionConstraints`, assigned to the `constraints` property of the `Trackable` object:

```kotlin
val exampleConstraints = DefaultResolutionConstraints(
  DefaultResolutionSet(
    Resolution(
      accuracy = Accuracy.BALANCED,
      desiredInterval = 1000L, // milliseconds
      minimumDisplacement = 1.0 // metres
    )
  ),
  proximityThreshold = DefaultProximity(spatial = 1.0), // metres
  batteryLevelThreshold = 10.0f, // percent
  lowBatteryMultiplier = 2.0f
)
```

These values are then used in the default `ResolutionPolicy`, created by the `DefaultResolutionPolicyFactory`. This default policy implementation uses a simple decision algorithm to determine the `Resolution` for a certain state, relative to proximity threshold, battery threshold and the presence of subscribers.

#### Providing a Custom Resolution Policy Implementation

For the greatest flexibility it is possible to provide a custom implementation of the `ResolutionPolicy` interface. In this implementation the application developer can define which logic will be applied to their own parameters, including how resolution is to be determined based on the those parameters and requests from subscribers.

Please see `DefaultResolutionPolicy` [implementation](publishing-sdk/src/main/java/com/ably/tracking/publisher/DefaultResolutionPolicyFactory.kt) for an example.

### Debugging Gradle Task Dependencies

There isn't an out-of-the-box command provided by Gradle to provide a readable breakdown of which tasks in the build are configured to rely upon which other tasks. The `--dry-run` switch helps a bit, but it provides a flat view which doesn't provide the full picture.

We could have taken the option to include some Groovy code or a plugin in the root project configuration to provide a full task tree view, however it's strictly not needed to be part of the sources within this repository to build the projects as it's only a tool to help with debugging Gradle's configuration.

If such a view is required then we suggest installing [this Gradle-global, user-level init script](https://github.com/dorongold/gradle-task-tree#init-script-snippet), within `~/.gradle/init.gradle` as [described in the Gradle documentation](https://docs.gradle.org/current/userguide/init_scripts.html#sec:using_an_init_script). Once the init script is in place then, for example, the Gradle `check` task can be examined using:

    ./gradlew check taskTree --no-repeat

The `taskTree` task requires a preceding task name and can be run per project, a fact that's visible with:

    ./gradlew tasks --all | grep taskTree
