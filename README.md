# Ably Asset Tracking SDKs for Android

![.github/workflows/check.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/check.yml/badge.svg)
![.github/workflows/emulate.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/emulate.yml/badge.svg)
![.github/workflows/assemble.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/assemble.yml/badge.svg)

## Overview

Ably Asset Tracking SDKs provide an easy way to track multiple assets with realtime location updates powered by [Ably](https://ably.com/) realtime network and Mapbox [Navigation SDK](https://docs.mapbox.com/android/navigation/overview/) with location enhancement.

**Status:** this is a beta version of the SDKs. That means that it contains a subset of the final SDK functionality, and the APIs are subject to change. The latest release of the SDKs is available in the [Releases section](https://github.com/ably/ably-asset-tracking-android/releases) of this repository.

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

### Documentation

Visit the [Ably Asset Tracking](https://ably.com/documentation/asset-tracking) documentation for a complete API reference and code examples.

### Useful Resources

- [Introducing Ably Asset Tracking - public beta now available](https://ably.com/blog/ably-asset-tracking-beta)
- [Accurate Delivery Tracking with Navigation SDK + Ably Realtime Network](https://www.mapbox.com/blog/accurate-delivery-tracking)

## Usage

### Maven / Gradle Dependencies

We publish to [GitHub Packages](https://github.com/ably/ably-asset-tracking-android/packages/) for this repository.

Kotlin users will want to add either `publishing-sdk` or `subscribing-sdk`, according to the needs of their project.
Java users should add either `publishing-sdk-java` or `subscribing-sdk-java`.
See [Android Runtime Requirements](#android-runtime-requirements) for more details.

### Publishing SDK

The Asset Publishing SDK is used to get the location of the assets that need to be tracked.

Here is an example of how the Asset Publishing SDK can be used:

```kotlin
// Prepare Resolution Constraints for an asset that will be used in the Resolution Policy
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

// Prepare the default resolution for the Resolution Policy
val defaultResolution = Resolution(Accuracy.BALANCED, desiredInterval = 1000L, minimumDisplacement = 1.0)

// Initialise and Start the Publisher
val publisher = Publisher.publishers() // get the Publisher builder in default state
    .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY))) // provide Ably configuration with credentials
    .map(MapConfiguration(MAPBOX_ACCESS_TOKEN)) // provide Mapbox configuration with credentials
    .androidContext(this) // provide Android runtime context
    .resolutionPolicy(DefaultResolutionPolicyFactory(defaultResolution, this)) // provide either the default resolution policy factory or your custom implementation
    .profile(RoutingProfile.DRIVING) // provide mode of transportation for better location enhancements
    .start()

// Start tracking an asset
try {
    publisher.track(
        Trackable(
            trackingId, // provide a tracking identifier for the asset
            constraints = exampleConstraints // provide a set of Resolution Constraints
        )
    )
    // TODO handle asset tracking started successfully
} catch (exception: Exception) {
    // TODO handle asset tracking could not be started
}
```

### Subscribing SDK

Asset Subscribing SDK is used to receive the location of the required assets.

Here is an example of how Asset Subscribing SDK can be used:

```kotlin
// Initialise and Start the Subscriber
val subscriber = Subscriber.subscribers() // Get an AssetSubscriber
    .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY))) // provide Ably configuration with credentials
    .resolution( // request a specific resolution to be considered by the publisher
        Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
    )
    .trackingId(trackingId) // provide the tracking identifier for the asset that needs to be tracked
    .start() // start listening for updates

// Listen for location updates
locations
    .onEach { locationUpdate -> print(locationUpdate } // provide a function to be called when enhanced location updates are received
    .launchIn(scope) // coroutines scope on which the locations are received

// Listen for asset state changes
trackableStates
    .onEach { trackableState -> print(trackableState) } // provide a function to be called when the asset changes its state
    .launchIn(scope) // coroutines scope on which the statuses are received

// Request a different resolution when needed.
try {
    subscriber.resolutionPreference(Resolution(Accuracy.MAXIMUM, desiredInterval = 100L, minimumDisplacement = 2.0))
    // TODO change request submitted successfully
} catch (exception: Exception) {
    // TODO change request could not be submitted
}
```

## Example Apps

This repository also contains example apps that showcase how the Ably Asset Tracking SDKs can be used:

- the [Asset Publishing example app](publishing-example-app/)
- the [Asset Subscribing example app](subscribing-example-app/)

To build these apps from source you will need to specify credentials in Gradle properties.

The following secrets need to be injected into Gradle by either storing them in `~/.gradle/gradle.properties`, or by using one of [many other ways](https://docs.gradle.org/current/userguide/build_environment.html) to do this:

 - `ABLY_API_KEY`: On your [Ably accounts page](https://ably.com/accounts/), select your application, and paste an API Key from the API Keys tab (with relevant capabilities for either subscriber/ publisher). This API key needs the following capabilities: `publish`, `subscribe`, `history` and `presence`.
 - `MAPBOX_DOWNLOADS_TOKEN`: On the [Mapbox Access Tokens page](https://account.mapbox.com/access-tokens/), create a token with the `DOWNLOADS:READ` secret scope.
 - `MAPBOX_ACCESS_TOKEN`: On the [Mapbox Access Tokens page](https://account.mapbox.com/access-tokens/), create a token with all public scopes or use the default public token automatically generated for you.
 - `GOOGLE_MAPS_API_KEY`: Create an API key in Google Cloud, ensuring it has both `Geolocation` and `Maps SDK for Android` API.

To do this, create a file in your home folder if it doesn't exist already, `~/.gradle/gradle.properties`, add the following code, and update the values:
```bash
ABLY_API_KEY=get_value_from_ably_dashboard
MAPBOX_DOWNLOADS_TOKEN=create_token_with_downloads_read_secret_scope
MAPBOX_ACCESS_TOKEN=create_token_with_all_public_scopes
GOOGLE_MAPS_API_KEY=create_api_key_with_geolocation_maps_sdk
```

## Android Runtime Requirements

### Kotlin Users

These SDKs require a minimum of Android API Level 21 at runtime for applications written in Kotlin.

### Java Users

We also provide support for applications written in Java, however the requirements differ in that case:
- must wrap using the appropriate Java facade for the SDK they are using:
    - [publishing-sdk-java](publishing-sdk-java/) for the [publishing-sdk](publishing-sdk/)
    - [subscribing-sdk-java](subscribing-sdk-java/) for the [subscribing-sdk](subscribing-sdk/)
- require Java 1.8 or later
- require a minimum of Android API Level 24 at runtime

## Resolution Policies

In order to provide application developers with flexibility when it comes to choosing their own balance between higher frequency of updates and optimal battery usage, we provide several ways for them to define the logic used to determine the frequency of updates:

- by implementing a custom `ResolutionPolicy` - providing the greatest flexibility
- by using the default `ResolutionPolicy` implementation - with the controls provided by `DefaultResolutionPolicyFactory` and `DefaultResolutionConstraints`

### Using the Default Resolution Policy

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

### Providing a Custom Resolution Policy Implementation

For the greatest flexibility it is possible to provide a custom implementation of the `ResolutionPolicy` interface. In this implementation the application developer can define which logic will be applied to their own parameters, including how resolution is to be determined based on the those parameters and requests from subscribers.

Please see `DefaultResolutionPolicy` [implementation](publishing-sdk/src/main/java/com/ably/tracking/publisher/DefaultResolutionPolicyFactory.kt) for an example.

## Contributing

For guidance on how to contribute to this project, see [CONTRIBUTING.md](CONTRIBUTING.md).
