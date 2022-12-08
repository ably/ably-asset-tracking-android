# Ably Asset Tracking SDKs for Android

![.github/workflows/check.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/check.yml/badge.svg)
![.github/workflows/emulate.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/emulate.yml/badge.svg)
![.github/workflows/assemble.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/assemble.yml/badge.svg)

_[Ably](https://ably.com) is the platform that powers synchronized digital experiences in realtime. Whether attending an event in a virtual venue, receiving realtime financial information, or monitoring live car performance data – consumers simply expect realtime digital experiences as standard. Ably provides a suite of APIs to build, extend, and deliver powerful digital experiences in realtime for more than 250 million devices across 80 countries each month. Organizations like Bloomberg, HubSpot, Verizon, and Hopin depend on Ably’s platform to offload the growing complexity of business-critical realtime data synchronization at global scale. For more information, see the [Ably documentation](https://ably.com/docs)._

## Overview

Ably Asset Tracking SDKs provide an easy way to track multiple assets with realtime location updates powered by [Ably](https://ably.com/) realtime network and Mapbox [Navigation SDK](https://docs.mapbox.com/android/navigation/overview/) with location enhancement.

Ably Asset Tracking is:

- **easy to integrate** - comprising two complementary SDKs with easy to use APIs, available for multiple platforms:
    - Asset Publishing SDK, for embedding in apps running on the courier's device
    - Asset Subscribing SDK, for embedding in apps running on the customer's observing device
- **extensible** - as Ably is used as the underlying transport, you have direct access to your data and can use Ably integrations for a wide range of applications in addition to direct realtime subscriptions - examples include:
    - passing to a 3rd party system
    - persistence for later retrieval
- **built for purpose** - the APIs and underlying functionality are designed specifically to meet the requirements of a range of common asset tracking use-cases

In this repository there are two SDKs for Android devices:

- the [Asset Publishing SDK](publishing-sdk/)
- the [Asset Subscribing SDK](subscribing-sdk/)

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

## Usage

Please see our [Upgrade / Migration Guide](UPDATING.md) for notes on changes you need to make to your code to update the Ably Asset Tracking SDKs.

If you're not writing your application in Kotlin then please see our [Java API Guide](JAVA.md) for notes on using the Ably Asset Tracking SDKs from Java code.

### Maven / Gradle Dependencies

Kotlin users will want to add either `publishing-sdk` or `subscribing-sdk`, according to the needs of their project.
Java users should add either `publishing-sdk-java` or `subscribing-sdk-java`.
See [Android Runtime Requirements](#android-runtime-requirements) for more details.

You need to declare the repository from which the Ably Asset Tracking SDK dependency will be installed.
We support both [Maven Central](#downloading-from-maven-central) and [GitHub Packages](#downloading-from-github-packages).

#### Downloading from Maven Central

We publish to [Maven Central](https://repo1.maven.org/maven2/com/ably/tracking/),
which is the public repository that most users will choose to download the Ably Asset Tracking SDK from.

To install the dependency you need to make sure that you have [declared the Maven Central repository](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:maven_central) in your Gradle build script:

```groovy
repositories {
    mavenCentral()
}
```

#### Downloading from GitHub Packages

We publish to [GitHub Packages](https://github.com/ably/ably-asset-tracking-android/packages/) for this repository,
which is an alternative option for those who do not wish to download the Ably Asset Tracking SDK from [Maven Central](#downloading-from-maven-central).

To [install the dependency](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package) you will first need to [authenticate to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).
You have to get either a `GITHUB_TOKEN` or a "Personal Access Token" (with the `read:packages` permission).
Then use that token to authenticate with the Ably Asset Tracking GitHub Packages repository in your Gradle build script:

```groovy
repositories {
    maven {
        name = "Ably Asset Tracking"
        url = uri("https://maven.pkg.github.com/ably/ably-asset-tracking-android")
        credentials {
            username = '<GITHUB_USERNAME>'
            password = '<GITHUB_TOKEN>'
        }
    }
}
```

#### Downloading Transitive Dependencies from the Mapbox Repository

In order to resolve all dependencies required by the Ably Asset Tracking SDK, you will also need to authenticate with the Mapbox repository in your Gradle build script:

```groovy
repositories {
    maven {
        name = "Mapbox"
        url 'https://api.mapbox.com/downloads/v2/releases/maven'
        authentication {
            basic(BasicAuthentication)
        }
        credentials {
            username = '<MAPBOX_USERNAME>'
            password = '<MAPBOX_DOWNLOADS_TOKEN>'
        }
    }
}
```

#### Adding Implementation Dependencies

Once you have configured Gradle to know where it can download dependencies from (see above),
you can then add the Ably Asset Tracking dependency that you require in your Gradle build script:

```groovy
dependencies {
    // Publishers, developing in Kotlin, will need the Publishing SDK
    implementation 'com.ably.tracking:publishing-sdk:1.5.1'

    // Subscribers, developing in Kotlin, will need the Subscribing SDK
    implementation 'com.ably.tracking:subscribing-sdk:1.5.1'

    // Subscribers, developing in Kotlin, can optionally use the UI utilities
    implementation 'com.ably.tracking:ui-sdk:1.5.1'
}
```

It's likely that for most application use cases you will need one or the other
(i.e. _either_ the Publishing SDK, _or_ the Subscribing SDK).

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
    // Required configuration
    .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY))) // provide Ably configuration with credentials
    .map(MapConfiguration(MAPBOX_ACCESS_TOKEN)) // provide Mapbox configuration with credentials
    .androidContext(this) // provide Android runtime context
    .resolutionPolicy(DefaultResolutionPolicyFactory(defaultResolution, this)) // provide either the default resolution policy factory or your custom implementation
    .backgroundTrackingNotificationProvider(
      object : PublisherNotificationProvider {
        override fun getNotification(): Notification {
            // TODO: create the notification for location updates background service
        }
      },
      NOTIFICATION_ID
    )
    // Optional configuration
    .profile(RoutingProfile.DRIVING) // provide mode of transportation for better location enhancements
    .logHandler(object : LogHandler {
        override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
          // TODO: log the message to internal or external loggers
        }
      })
    .rawLocations(false) // send raw location updates to subscribers
    .sendResolution(true) // send calculated trackable network resolution to subscribers
    .constantLocationEngineResolution(constantLocationEngineResolution) // provide a constant resolution for the GPS engine
    .vehicleProfile(VehicleProfile.CAR) // provide vehicle type for better location enhancements
    .locationSource(LocationSourceRaw.create(historyData)) // use an alternative location source for GPS locations
    // Create and start the publisher
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
    // Required configuration
    .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY))) // provide Ably configuration with credentials
    .trackingId(trackingId) // provide the tracking identifier for the asset that needs to be tracked
    // Optional configuration
    .resolution( // request a specific resolution to be considered by the publisher
      Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
    )
    .logHandler(object : LogHandler {
      override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
        // TODO: log the message to internal or external loggers
      }
    })
    // Create and start the subscriber
    .start() // start listening for updates

// Listen for location updates
locations
    .onEach { locationUpdate -> print(locationUpdate) } // provide a function to be called when enhanced location updates are received
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

### Publisher presence

Publisher presence is provided as an experimental API for subscribers which you can use to get information about
whether the publisher is online or offline. This API is not yet stable and may change in the future.

To use the API you must explicitly opt in to it by adding the following to your `build.gradle` file:

```groovy
android {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}
```

Then you can annotate your element of the desired scope with ``@OptIn(Experimental::class)`` annotation.

An example usage of the API is shown below:

```kotlin
subscriber.publisherPresence
    .onEach { isOnline -> print(isOnline) } // provide a function to be called when the asset's presnec is changed
    .launchIn(scope) // coroutines scope on which the statuses are received
```

### UI utilities

#### Location Animator

The Location Animator helps to achieve smooth trackable animations on the subscriber side. For more information see our [updating guide](UPDATING.md#Animation module).

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

### Resources

Visit the [Ably Asset Tracking](https://ably.com/docs/asset-tracking) documentation for a complete API reference and code examples.

You can also find reference documentation generated from the source code [here](https://sdk.ably.com/builds/ably/ably-asset-tracking-android/main/dokka/index.html).

#### Upgrade / Migration Guide

Please see our [Upgrade / Migration Guide](UPDATING.md) for notes on changes you need to make to your code to update it to use the latest version of these SDKs.

#### Useful links

- [Introducing Ably Asset Tracking - public beta now available](https://ably.com/blog/ably-asset-tracking-beta)
- [Accurate Delivery Tracking with Navigation SDK + Ably Realtime Network](https://www.mapbox.com/blog/accurate-delivery-tracking)

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

## Known Limitations

### Using AAT together with Mapbox Navigation SDK

There are some limitations when you want to use both AAT SDK and Mapbox Navigation SDK in the same project.

Firstly, you have to exclude the notification module from Mapbox Navigation SDK dependency in your `build.gradle` file.

```groovy
// The Ably Asset Tracking Publisher SDK for Android.
implementation ('com.ably.tracking:publishing-sdk:1.5.1')

// The Mapbox Navigation SDK.
implementation ('com.mapbox.navigation:android:2.8.0') {
    exclude group: "com.mapbox.navigation", module: "notification"
}
```

Secondly, you have to use AAT's `MapboxNavigation` configuration and make sure that a publisher is started before you try to use the Mapbox Navigation.
As there can only be one `MapboxNavigation` instance per application, instead of creating a new instance you have to use the `MapboxNavigationProvider` to retrieve the instance created by AAT.

```kotlin
// Start a publisher before accessing Mapbox Navigation SDK
val publisher = Publisher.publishers()
    // add publisher configuration
    .start()

// Retrieve the instance created by the publisher
val mapboxNavigation = MapboxNavigationProvider.retrieve()
```

Because there is only one `MapboxNavigation` instance, both your app and AAT will use the same object. This means that there can be possible conflicts in usage that can lead to unexpected behaviour.
Therefore, we do not advise using AAT in applications that already use Mapbox Navigation SDK.

### SLF4J warning logs

AAT has a transitive dependency on the [SLF4J](https://www.slf4j.org/) library but we do not provide a default logger implementation. Because of that you can encounter below warning logs if you have not explicitly provided an implementation:

```
W/System.err: SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
W/System.err: SLF4J: Defaulting to no-operation (NOP) logger implementation
```

This is normal behaviour if no specific SLF4J logger implementation is provided. If you want to fix those warnings you have to provide a SLF4J logger implementation in your project.

### Using multiple publishers at the same time

While it's possible to create multiple publishers and use them at the same time it is not advised. The reason is that the underlying location service is created when the publisher is created and it is destroyed when the publisher is stopped.
Therefore, if you create a publisher after another publisher was created but not stopped, the new publisher's location engine configuration won't be applied, since the location service is already created and running.
Additionally, since all publishers use the same location service instance, when one of them stops location updates (by removing its last trackable) they will stop for all of them.

All the above issues won't occur when you create a new publisher after stopping the previous one, which is the recommended way of using the SDK.

## Contributing

For guidance on how to contribute to this project, see [CONTRIBUTING.md](CONTRIBUTING.md).
