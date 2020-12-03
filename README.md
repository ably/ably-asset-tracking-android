## Ably Asset Tracking SDKs for Android

![.github/workflows/check.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/check.yml/badge.svg)
![.github/workflows/check.yml](https://github.com/ably/ably-asset-tracking-android/workflows/.github/workflows/assemble.yml/badge.svg)

### Overview

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
// Initialise the Publisher
publisher = Publisher.publishers() // get a Publisher
  .ably(AblyConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
  .map(MapConfiguration(MAPBOX_ACCESS_TOKEN)) // provide Mapbox configuration with credentials
  .androidContext(this) // provide context
  .mode(TransportationMode("bike")) //provide mode of transportation for better location enhancements
  .start() 
  
// Start tracking asset
publisher.track(Trackable(trackingId)) // provide a tracking ID of the asset
```

Asset Subscribing SDK is used to receive the location of the required assets. 

Here is an example of how Asset Subscribing SDK can be used: 

```kotlin
assetSubscriber = AssetSubscriber.subscribers() // Get an AssetSubscriber
  .ablyConfig(AblyConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
  .rawLocationUpdatedListener {} // provide a function to be called when raw location updates are received
  .enhancedLocationUpdatedListener {} // provide a function to be called when enhanced location updates are received
  .trackingId(trackingId) // provide a Trackable ID for the asset that needs to be tracked
  .assetStatusListener { } // provide a function to be called when asset changes online/offline status
  .start() // start listening to updates
```

### Example Apps

This repository also contains example apps that showcase how Ably Asset Tracking SDKs can be used:

- the [Asset Publishing example app](publishing-example-app/)
- the [Asset Subscribing example app](subscribing-example-app/)

To build the apps you will need to specify [credentials](#api-keys-and-access-tokens) in Gradle properties.


### Development

This repository is structured as a Gradle [Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html).

We'll add more content here as the development evolves. For the time being this content will be focussed on those developing the code within this repository. Eventually it'll move elsewhere so that we can replace this root readme with something public facing.

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
